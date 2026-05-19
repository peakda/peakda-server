package com.peakda.server.domain.user.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageFormat
import com.peakda.server.common.image.ImageVariant
import org.springframework.web.multipart.MultipartFile

object ProfileImagePolicy {
    val MAX_FILE_SIZE_BYTES: Long = 5 * 1024 * 1024
    val ALLOWED_MIME_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp")

    val VARIANTS: List<ImageVariant> = listOf(
        ImageVariant(name = "thumbnail", width = 128, height = 128, format = ImageFormat.JPEG),
        ImageVariant(name = "main", width = 512, height = 512, format = ImageFormat.JPEG),
    )

    const val MAIN_VARIANT = "main"

    fun keyOf(userId: Long, variant: ImageVariant): String =
        "profile-images/$userId/${variant.name}.${variant.format.extension}"

    fun validate(file: MultipartFile) {
        if (file.isEmpty) throw ImageException(ErrorCode.IMAGE_REQUIRED)
        if (file.size > MAX_FILE_SIZE_BYTES) throw ImageException(ErrorCode.IMAGE_SIZE_EXCEEDED)
        val contentType = file.contentType?.lowercase()
        if (contentType !in ALLOWED_MIME_TYPES) throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
    }
}
