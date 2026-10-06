package com.peakda.server.domain.curation.application

import com.peakda.server.common.image.ImageResizer
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.storage.ObjectStorage
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth
import java.util.UUID

@Component
class CurationImageUploader(
    private val objectStorage: ObjectStorage,
    private val imageResizer: ImageResizer,
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {

    fun upload(file: MultipartFile, usage: CurationImageUsage): UploadedImage {
        CurationImagePolicy.validate(file)
        val variant = CurationImagePolicy.variantOf(usage)
        val resized = imageResizer.resize(file.bytes, listOf(variant)).single()
        val prefix = CurationImagePolicy.prefixOf(UUID.randomUUID().toString(), YearMonth.now())
        val objectKey = CurationImagePolicy.keyOf(prefix, variant)
        objectStorage.upload(objectKey, resized.bytes, variant.format.mimeType)
        return UploadedImage(
            objectKey = objectKey,
            previewUrl = objectKeyUrlResolver.resolveKey(objectKey),
        )
    }

    data class UploadedImage(
        val objectKey: String,
        val previewUrl: String,
    )
}
