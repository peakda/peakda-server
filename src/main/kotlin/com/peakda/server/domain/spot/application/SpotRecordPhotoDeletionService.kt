package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.spot.entity.PhotoDeletionStatus
import com.peakda.server.domain.spot.entity.SpotRecordPhotoDeletion
import com.peakda.server.domain.spot.repository.SpotRecordPhotoDeletionRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.util.UUID

@Service
class SpotRecordPhotoDeletionService(
    private val jobs: SpotRecordPhotoDeletionRepository,
    private val uploads: SpotRecordPhotoUploadRepository,
    private val photos: SpotRecordPhotoRepository,
    private val storage: ObjectStorage,
    private val properties: SpotRecordPhotoDeletionProperties,
    private val meters: MeterRegistry,
    transactionManager: PlatformTransactionManager,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val log = LoggerFactory.getLogger(javaClass)
    // 원본 참조 해제와 별개 트랜잭션: 미커밋 예약은 보지 않으며 variant 진행은 각각 커밋한다.
    private val transactions = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun drain(): Int {
        var processed = 0
        repeat(properties.batchSize) {
            val claim = claimNext() ?: return processed
            process(claim)
            processed++
        }
        return processed
    }

    fun outstandingCounts(): Map<String, Long> = jobs.countOutstandingByStatus().associate { it.status.name to it.total }

    fun claimNext(): SpotRecordPhotoDeletionClaim? = transactions.execute {
        repeat(properties.batchSize) {
            val now = clock.instant()
            val job = jobs.findNextForUpdate(now) ?: return@execute null
            if (job.attempts >= properties.maxAttempts) {
                fail(job, "ATTEMPTS_EXHAUSTED_AFTER_LEASE", permanent = true)
            } else {
                job.status = PhotoDeletionStatus.PROCESSING
                job.attempts++
                job.totalAttempts++
                job.claimToken = UUID.randomUUID()
                job.leaseUntil = now.plus(properties.lease)
                return@execute SpotRecordPhotoDeletionClaim(requireNotNull(job.id), job.objectKey, requireNotNull(job.claimToken))
            }
        }
        null
    }

    /** false면 stale claim이다. 호출자가 이전 worker여도 스토리지/진행을 갱신하지 않는다. */
    fun process(claim: SpotRecordPhotoDeletionClaim): Boolean {
        while (true) {
            val more = transactions.execute {
                // 작성/backfill의 record → object 순서와 충돌하지 않는다. 이 경로는 record를 잠그지 않는다.
                val upload = uploads.findByKeyForUpdate(claim.objectKey)
                val job = jobs.findByIdForUpdate(claim.id) ?: return@execute null
                if (job.objectKey != claim.objectKey || !job.ownsClaim(claim.token, clock.instant())) return@execute null
                if (upload == null || upload.ownerId != job.ownerId || upload.evidence != job.evidence ||
                    !SpotRecordPhotoPolicy.isServerMainKey(job.ownerId, job.objectKey) ||
                    job.evidence !in setOf("SERVER_UPLOAD", "LEGACY_SERVER_KEY_AND_STORAGE") ||
                    SpotRecordPhotoPolicy.availableVariantNames(job.variantNames).any { it !in setOf("thumbnail", "medium", "main") }
                ) {
                    fail(job, "OWNERSHIP_OR_VARIANTS_UNVERIFIABLE", permanent = true)
                    return@execute false
                }
                if (photos.findReferencedObjectKeys(listOf(job.objectKey)).isNotEmpty()) {
                    if (job.completedVariants.isNotEmpty()) {
                        fail(job, "REFERENCE_AFTER_PARTIAL_DELETE", permanent = true)
                    } else {
                        // 종료한 예약이 이후 마지막 참조의 새 예약을 막지 않는다.
                        upload.available = true
                        job.finish(clock.instant(), "REFERENCED")
                    }
                    return@execute false
                }
                upload.available = false
                val completed = job.completedVariants.split(',').filter(String::isNotEmpty).toSet()
                val variant = SpotRecordPhotoPolicy.availableVariantNames(job.variantNames).firstOrNull { it !in completed }
                if (variant == null) {
                    job.finish(clock.instant(), "DELETED")
                    meters.counter("spot.photo.deletion.completed").increment()
                    return@execute false
                }
                job.leaseUntil = clock.instant().plus(properties.lease)
                val key = "${job.objectKey.substringBeforeLast('/')}/$variant.jpg"
                try {
                    // S3 DELETE는 이미 없는 key도 성공이다. HEAD 실패를 없음으로 취급하지 않는다.
                    storage.delete(key)
                    if (!job.ownsClaim(claim.token, clock.instant())) return@execute null
                    val progressed = completed + variant
                    job.completedVariants = progressed.joinToString(",")
                    if (progressed.containsAll(SpotRecordPhotoPolicy.availableVariantNames(job.variantNames))) {
                        job.finish(clock.instant(), "DELETED")
                        meters.counter("spot.photo.deletion.completed").increment()
                        false
                    } else true
                } catch (error: Exception) {
                    if (!job.ownsClaim(claim.token, clock.instant())) return@execute null
                    // 기존 storage adapter는 원인 HTTP status를 노출하지 않는다. 불투명한 장애는 bounded retry.
                    fail(job, error.javaClass.simpleName, error is IllegalArgumentException || error is SecurityException)
                    false
                }
            } ?: return false
            if (!more) return true
        }
    }

    /** 내부 운영 복구용. 진행/누적 횟수/오류는 보존하고 다음 bounded 시도만 연다. 공개 API는 추가하지 않는다. */
    fun retryFailed(id: Long): Boolean = transactions.execute {
        val job = jobs.findByIdForUpdate(id) ?: return@execute false
        if (job.status != PhotoDeletionStatus.FAILED) return@execute false
        job.status = PhotoDeletionStatus.RETRYABLE
        job.attempts = 0
        job.reprocessCount++
        job.nextAttemptAt = clock.instant()
        job.finishedAt = null
        log.warn("사진 삭제 내부 재처리 job={} totalAttempts={} reprocessCount={}", id, job.totalAttempts, job.reprocessCount)
        true
    } ?: false

    private fun fail(job: SpotRecordPhotoDeletion, reason: String, permanent: Boolean) {
        val now = clock.instant()
        job.lastError = reason.take(500)
        job.leaseUntil = null
        job.claimToken = null
        if (permanent || job.attempts >= properties.maxAttempts) {
            job.status = PhotoDeletionStatus.FAILED
            job.finishedAt = now
            meters.counter("spot.photo.deletion.failed").increment()
            log.error("사진 삭제 FAILED job={} attempts={} reason={}", job.id, job.attempts, job.lastError)
        } else {
            job.status = PhotoDeletionStatus.RETRYABLE
            job.nextAttemptAt = now.plus(properties.backoff(job.attempts))
            meters.counter("spot.photo.deletion.retryable").increment()
            log.warn("사진 삭제 RETRYABLE job={} attempts={} next={} reason={}", job.id, job.attempts, job.nextAttemptAt, job.lastError)
        }
    }
}
