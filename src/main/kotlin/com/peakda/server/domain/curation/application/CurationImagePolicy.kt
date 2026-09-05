package com.peakda.server.domain.curation.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageVariant
import com.peakda.server.common.image.ResizeMode
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth

object CurationImagePolicy {
    const val MAX_FILE_SIZE_BYTES: Long = 10L * 1024 * 1024
    val ALLOWED_MIME_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp")

    val VARIANTS: List<ImageVariant> = listOf(
        ImageVariant(name = "thumbnail", width = 640, height = 360, mode = ResizeMode.FIT),
        ImageVariant(name = "main", width = 1600, height = 900, mode = ResizeMode.FIT),
    )

    const val MAIN_VARIANT: String = "main"

    fun keyOf(prefix: String, variant: ImageVariant): String =
        "$prefix/${variant.name}.${variant.format.extension}"

    fun prefixOf(uuid: String, yearMonth: YearMonth): String =
        "curations/$yearMonth/$uuid"

    fun validate(file: MultipartFile) {
        if (file.isEmpty) throw ImageException(ErrorCode.IMAGE_REQUIRED)
        if (file.size > MAX_FILE_SIZE_BYTES) throw ImageException(ErrorCode.IMAGE_SIZE_EXCEEDED)
        val contentType = file.contentType?.lowercase()
        if (contentType !in ALLOWED_MIME_TYPES) throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
    }
}
