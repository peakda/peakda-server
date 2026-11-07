package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.spot.entity.SpotRecordPhotoUpload
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoUploadRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.MANDATORY)
class SpotRecordPhotoAccessService(
    private val uploads: SpotRecordPhotoUploadRepository,
    private val photos: SpotRecordPhotoRepository,
    private val storage: ObjectStorage,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 모든 변경 경로가 동일한 순서로 잠근다. 반환한 행은 호출 트랜잭션이 끝날 때까지 잠긴다. */
    fun lockForChange(
        userId: Long,
        existingKeys: Set<String>,
        nextKeys: Set<String>,
    ): Map<String, SpotRecordPhotoUpload> = buildMap {
        (existingKeys + nextKeys).sorted().forEach { key ->
            var upload = uploads.findByKeyForUpdate(key)
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

    /** 참조 해제를 flush한 뒤, 소유자가 일치하는 마지막 참조만 물리 삭제한다. */
    fun deleteUnreferenced(userId: Long, keys: Set<String>, locked: Map<String, SpotRecordPhotoUpload>) {
        photos.flush()
        keys.sorted().forEach { key ->
            val upload = locked[key] ?: return@forEach
            if (upload.ownerId != userId || !upload.available || photos.existsByObjectKey(key)) return@forEach
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
}
