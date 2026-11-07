package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(propagation = Propagation.MANDATORY)
class SpotRecordPhotoAccessService(
    private val uploads: SpotRecordPhotoUploadRepository,
    private val photos: SpotRecordPhotoRepository,
    private val storage: ObjectStorage,
    private val records: SpotRecordRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 모든 변경 경로가 동일한 순서로 잠근다. 반환한 행은 호출 트랜잭션이 끝날 때까지 잠긴다. */
    fun lockForChange(
        userId: Long,
        existingKeys: Set<String>,
        nextKeys: Set<String>,
    ): Map<String, SpotRecordPhotoUpload> {
        val orderedKeys = (existingKeys + nextKeys).sorted()
        val mayRegister = nextKeys.any { it !in existingKeys }
        // 신규 등록이 있으면 낮은 key 삽입보다 높은 key를 먼저 잠그지 않도록 순차 획득한다.
        val batchLocked = if (mayRegister) emptyMap() else orderedKeys.chunked(KEY_QUERY_BATCH_SIZE)
            .flatMap { uploads.findByKeysForUpdate(it) }.associateBy { it.objectKey }
        return buildMap {
            orderedKeys.forEach { key ->
                var upload = if (mayRegister) uploads.findByKeyForUpdate(key) else batchLocked[key]
                if (upload == null && key in nextKeys && key !in existingKeys) {
                    val variants = verifyLegacy(userId, key)
                    uploads.registerLegacyIfAbsent(key, userId, variants)
                    upload = uploads.findByKeyForUpdate(key)
                }
                // 미등록 기존 연결은 소유 증거가 아니다. 재정렬만 허용하고 삭제에는 사용하지 않는다.
                if (key in nextKeys && upload != null && !upload.available) throw SpotRecordForbiddenException()
                if (key in nextKeys && key !in existingKeys &&
                    (upload == null || upload.ownerId != userId || !upload.available)
                ) throw SpotRecordForbiddenException()
                if (upload != null) put(key, upload)
            }
        }
    }

    fun lockForBackfill(key: String): SpotRecordPhotoUpload {
        val ownerId = SpotRecordPhotoPolicy.ownerIdOfServerMainKey(key) ?: throw SpotRecordForbiddenException()
        // 최초 등록 전 레거시 재정렬도 막는다. 작성 경로와 같은 record → object 순서다.
        photos.findRecordIdsByObjectKey(key).sorted().chunked(KEY_QUERY_BATCH_SIZE)
            .forEach { records.findByIdsForUpdate(it) }
        return lockForChange(ownerId, emptySet(), setOf(key)).getValue(key)
    }

    /** 같은 객체 잠금 안에서 연결과 등록부의 보유 variant를 함께 갱신한다. */
    fun updateBackfilledVariants(upload: SpotRecordPhotoUpload) {
        if (!upload.available) throw SpotRecordForbiddenException()
        photos.updateVariantsByObjectKey(upload.objectKey, SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES, Instant.now())
        upload.variantNames = SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES
    }

    /** 참조 해제를 flush한 뒤, 소유자가 일치하는 마지막 참조만 물리 삭제한다. */
    fun deleteUnreferenced(userId: Long, keys: Set<String>, locked: Map<String, SpotRecordPhotoUpload>) {
        photos.flush()
        val ownedUploads = keys.sorted().mapNotNull(locked::get).filter { it.ownerId == userId && it.available }
        val referencedKeys = ownedUploads.map { it.objectKey }
            .chunked(KEY_QUERY_BATCH_SIZE)
            .flatMap { photos.findReferencedObjectKeys(it) }
            .toSet()
        ownedUploads.forEach { upload ->
            val key = upload.objectKey
            if (key in referencedKeys) return@forEach
            upload.available = false
            SpotRecordPhotoPolicy.availableVariantNames(upload.variantNames).forEach { name ->
                val variantKey = "${key.substringBeforeLast('/')}/$name.jpg"
                runCatching { storage.delete(variantKey) }
                    .onFailure { log.warn("기록 사진 삭제 실패 key={}", variantKey, it) }
            }
        }
    }

    private fun verifyLegacy(userId: Long, key: String): String {
        if (!SpotRecordPhotoPolicy.isServerMainKey(userId, key)) throw SpotRecordForbiddenException()
        // 서버 전용 생성/쓰기 정책을 전제로 한 legacy 증거다. association이나 prefix만 신뢰하지 않는다.
        val prefix = key.substringBeforeLast('/')
        if (!storage.exists(key) || !storage.exists("$prefix/thumbnail.jpg")) {
            throw SpotRecordForbiddenException()
        }
        val hasMedium = storage.exists("$prefix/medium.jpg")
        return if (hasMedium) SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES else "thumbnail,main"
    }

    private companion object {
        // 바인딩 폭만 제한하는 내부 조회 단위다. 전체 key를 모두 처리한다.
        const val KEY_QUERY_BATCH_SIZE = 1_000
    }
}
