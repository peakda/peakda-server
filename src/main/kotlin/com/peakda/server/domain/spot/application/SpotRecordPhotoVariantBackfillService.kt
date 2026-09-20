package com.peakda.server.domain.spot.application

import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectStorage
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * variant 세트가 늘어나기 전에 올라온 기록 사진에 빠진 이미지를 채워 넣는 일회성 작업.
 *
 * 원본(main)만 내려받아 나머지 variant 를 다시 만든다. 한 번에 다 돌리지 않고 배치로 끊어
 * 관리자가 반복 호출하는 방식이며, [SpotRecordPhotoVariantBackfillResult.remaining] 이 0 이 되면 끝이다.
 */
@Service
class SpotRecordPhotoVariantBackfillService(
    private val spotRecordPhotoRepository: SpotRecordPhotoRepository,
    private val objectStorage: ObjectStorage,
    private val imageResizer: ImageResizer,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun backfill(batchSize: Int): SpotRecordPhotoVariantBackfillResult {
        val targets = spotRecordPhotoRepository
            .findByVariantNamesIsNullOrderByIdAsc(PageRequest.of(0, batchSize.coerceIn(MIN_BATCH_SIZE, MAX_BATCH_SIZE)))

        var updated = 0
        var failed = 0
        targets.forEach { photo ->
            if (regenerate(photo)) updated++ else failed++
        }

        return SpotRecordPhotoVariantBackfillResult(
            scanned = targets.size,
            updated = updated,
            failed = failed,
            remaining = spotRecordPhotoRepository.countByVariantNamesIsNull(),
        )
    }

    /** 원본은 그대로 두고 나머지 variant 만 다시 만든다. 한 장이 실패해도 배치 전체를 멈추지 않는다. */
    private fun regenerate(photo: SpotRecordPhoto): Boolean =
        runCatching {
            val source = objectStorage.download(photo.objectKey)
            val derived = SpotRecordPhotoPolicy.VARIANTS.filterNot { it.name == SpotRecordPhotoPolicy.MAIN_VARIANT }
            imageResizer.resize(source, derived).forEach { result ->
                val key = SpotRecordPhotoPolicy.variantKeyOf(photo.objectKey, result.variant)
                objectStorage.upload(key, result.bytes, result.variant.format.mimeType)
            }
            photo.variantNames = SpotRecordPhotoPolicy.CURRENT_VARIANT_NAMES
        }.onFailure { e ->
            log.warn("기록 사진 variant 백필 실패 photoId={} key={}", photo.id, photo.objectKey, e)
        }.isSuccess

    companion object {
        const val DEFAULT_BATCH_SIZE: Int = 50
        private const val MIN_BATCH_SIZE: Int = 1
        private const val MAX_BATCH_SIZE: Int = 200
    }
}
