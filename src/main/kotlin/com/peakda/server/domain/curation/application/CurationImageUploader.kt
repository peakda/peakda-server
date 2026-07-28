package com.peakda.server.domain.curation.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectStorage
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth
import java.util.UUID

@Component
class CurationImageUploader(
    private val objectStorage: ObjectStorage,
    private val imageResizer: ImageResizer,
) {

    fun upload(file: MultipartFile): UploadedImage {
        CurationImagePolicy.validate(file)
        val resized = imageResizer.resize(file.bytes, CurationImagePolicy.VARIANTS)
        val prefix = CurationImagePolicy.prefixOf(UUID.randomUUID().toString(), YearMonth.now())
        var mainKey: String? = null
        resized.forEach { result ->
            val key = CurationImagePolicy.keyOf(prefix, result.variant)
            objectStorage.upload(key, result.bytes, result.variant.format.mimeType)
            if (result.variant.name == CurationImagePolicy.MAIN_VARIANT) {
                mainKey = key
            }
        }
        val objectKey = mainKey ?: throw ImageException(ErrorCode.IMAGE_PROCESSING_FAILED)
        return UploadedImage(
            objectKey = objectKey,
            previewUrl = objectStorage.presignedGetUrl(objectKey),
        )
    }

    data class UploadedImage(
        val objectKey: String,
        val previewUrl: String,
    )
}
