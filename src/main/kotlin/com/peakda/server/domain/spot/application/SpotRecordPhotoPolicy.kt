package com.peakda.server.domain.spot.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageFormat
import com.peakda.server.common.image.ImageVariant
import com.peakda.server.common.image.ResizeMode
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth

object SpotRecordPhotoPolicy {
    const val MAX_FILES_PER_REQUEST: Int = 5
    const val MIN_FILES_PER_REQUEST: Int = 1
    const val MAX_FILE_SIZE_BYTES: Long = 10L * 1024 * 1024
    val ALLOWED_MIME_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp")

    val VARIANTS: List<ImageVariant> = listOf(
        ImageVariant(name = "thumbnail", width = 400, height = 400, mode = ResizeMode.CROP, format = ImageFormat.JPEG),
        ImageVariant(name = "main", width = 1600, height = 1600, mode = ResizeMode.FIT, format = ImageFormat.JPEG),
    )

    const val MAIN_VARIANT: String = "main"

    fun prefixOf(userId: Long, uuid: String, yearMonth: YearMonth): String {
        val yyyy = yearMonth.year.toString()
        val mm = yearMonth.monthValue.toString().padStart(2, '0')
        return "spot-records/$userId/$yyyy/$mm/$uuid"
    }

    fun keyOf(prefix: String, variant: ImageVariant): String =
        "$prefix/${variant.name}.${variant.format.extension}"

    fun validate(files: List<MultipartFile>) {
        if (files.size !in MIN_FILES_PER_REQUEST..MAX_FILES_PER_REQUEST) {
            throw ImageException(ErrorCode.SPOT_RECORD_PHOTO_LIMIT)
        }
        files.forEach { validate(it) }
    }

    fun validate(file: MultipartFile) {
        if (file.isEmpty) throw ImageException(ErrorCode.IMAGE_REQUIRED)
        if (file.size > MAX_FILE_SIZE_BYTES) throw ImageException(ErrorCode.IMAGE_SIZE_EXCEEDED)
        val contentType = file.contentType?.lowercase()
        if (contentType !in ALLOWED_MIME_TYPES) throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
    }
}
