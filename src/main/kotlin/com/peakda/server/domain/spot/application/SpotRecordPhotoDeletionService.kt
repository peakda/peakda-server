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
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.util.UUID

@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
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

    /** 외부 호출은 DB 잠금 없이 수행한다. stale claim의 결과는 반영하지 않는다. */
    fun process(claim: SpotRecordPhotoDeletionClaim): Boolean {
        var accepted = false
        while (true) {
            val variant = prepareVariant(claim) ?: return accepted
            val key = "${claim.objectKey.substringBeforeLast('/')}/$variant.jpg"
            // S3 DELETE는 이미 없는 key도 성공이다. 실패한 HEAD를 없음으로 취급하지 않는다.
            val failure = runCatching { storage.delete(key) }.exceptionOrNull()
            if (failure is Error) throw failure
            if (!recordResult(claim, variant, failure)) return false
            accepted = true
            if (failure != null) return true
        }
    }

    /** 삭제 시작을 먼저 커밋한다. 이후 lease가 바뀌어도 이 key를 다시 available로 열지 않는다. */
    private fun prepareVariant(claim: SpotRecordPhotoDeletionClaim): String? = transactions.execute {
        // 작성/backfill의 record → object 순서와 충돌하지 않는다. record를 역순으로 잠그지 않는다.
        val upload = uploads.findByKeyForUpdate(claim.objectKey)
        val job = jobs.findByIdForUpdate(claim.id) ?: return@execute null
        if (job.objectKey != claim.objectKey || !job.ownsClaim(claim.token, clock.instant())) return@execute null
        if (upload == null || upload.ownerId != job.ownerId || upload.evidence != job.evidence ||
            !SpotRecordPhotoPolicy.isServerMainKey(job.ownerId, job.objectKey) ||
            job.evidence !in setOf("SERVER_UPLOAD", "LEGACY_SERVER_KEY_AND_STORAGE") ||
            SpotRecordPhotoPolicy.availableVariantNames(job.variantNames).any { it !in setOf("thumbnail", "medium", "main") }
        ) {
            fail(job, "OWNERSHIP_OR_VARIANTS_UNVERIFIABLE", permanent = true)
            return@execute null
        }
        if (photos.findReferencedObjectKeys(listOf(job.objectKey)).isNotEmpty()) {
            if (job.deletionStarted || job.completedVariants.isNotEmpty()) {
                fail(job, "REFERENCE_AFTER_DELETE_STARTED", permanent = true)
            } else {
                // 아직 외부 삭제를 시작하지 않은 예약만 취소하고 이후 마지막 참조의 재예약을 허용한다.
                upload.available = true
                job.finish(clock.instant(), "REFERENCED")
            }
            return@execute null
        }
        upload.available = false
        job.deletionStarted = true
        val completed = job.completedVariants.split(',').filter(String::isNotEmpty).toSet()
        val variant = SpotRecordPhotoPolicy.availableVariantNames(job.variantNames).firstOrNull { it !in completed }
        if (variant == null) {
            job.finish(clock.instant(), "DELETED")
            return@execute null
        }
        job.leaseUntil = clock.instant().plus(properties.lease)
        variant
    }

    private fun recordResult(claim: SpotRecordPhotoDeletionClaim, variant: String, failure: Throwable?): Boolean =
        transactions.execute {
            val job = jobs.findByIdForUpdate(claim.id) ?: return@execute false
            if (job.objectKey != claim.objectKey || !job.ownsClaim(claim.token, clock.instant())) return@execute false
            if (failure != null) {
                // 기존 storage adapter는 원인 HTTP status를 노출하지 않는다. 불투명한 장애는 bounded retry.
                fail(job, failure.javaClass.simpleName, failure is IllegalArgumentException || failure is SecurityException)
            } else {
                val completed = job.completedVariants.split(',').filter(String::isNotEmpty).toSet() + variant
                job.completedVariants = completed.joinToString(",")
                if (completed.containsAll(SpotRecordPhotoPolicy.availableVariantNames(job.variantNames))) {
                    job.finish(clock.instant(), "DELETED")
                    meters.counter("spot.photo.deletion.completed").increment()
                }
            }
            true
        } ?: false

    /** 기존 관리자 수동 잡 전용: ID 페이지로 제한하고 자동 FAILED 반복 실행은 하지 않는다. */
    fun retryFailedBatch(): Int = jobs.findIdsByStatus(PhotoDeletionStatus.FAILED, PageRequest.of(0, properties.batchSize))
        .count { retryFailed(it) }

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
