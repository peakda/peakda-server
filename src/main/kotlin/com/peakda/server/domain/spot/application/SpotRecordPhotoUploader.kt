package com.peakda.server.domain.spot.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectStorage
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth
import java.util.UUID

@Component
class SpotRecordPhotoUploader(
    private val objectStorage: ObjectStorage,
    private val imageResizer: ImageResizer,
) {

    fun upload(userId: Long, files: List<MultipartFile>): List<UploadedPhoto> {
        SpotRecordPhotoPolicy.validate(files)
        val yearMonth = YearMonth.now()
        return files.map { uploadSingle(userId, it, yearMonth) }
    }

    fun presignedUrlOf(objectKey: String): String = objectStorage.presignedGetUrl(objectKey)

    fun deleteByMainKey(mainKey: String) {
        if (!mainKey.startsWith("spot-records/")) return
        val prefix = mainKey.substringBeforeLast("/")
        SpotRecordPhotoPolicy.VARIANTS.forEach { variant ->
            val key = SpotRecordPhotoPolicy.keyOf(prefix, variant)
            runCatching { objectStorage.delete(key) }
        }
    }

    private fun uploadSingle(userId: Long, file: MultipartFile, yearMonth: YearMonth): UploadedPhoto {
        val resized = imageResizer.resize(file.bytes, SpotRecordPhotoPolicy.VARIANTS)
        val prefix = SpotRecordPhotoPolicy.prefixOf(userId, UUID.randomUUID().toString(), yearMonth)
        var mainKey: String? = null
        resized.forEach { result ->
            val key = SpotRecordPhotoPolicy.keyOf(prefix, result.variant)
            objectStorage.upload(key, result.bytes, result.variant.format.mimeType)
            if (result.variant.name == SpotRecordPhotoPolicy.MAIN_VARIANT) {
                mainKey = key
            }
        }
        val key = mainKey ?: throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        return UploadedPhoto(objectKey = key, previewUrl = objectStorage.presignedGetUrl(key))
    }

    data class UploadedPhoto(
        val objectKey: String,
        val previewUrl: String,
    )
}
