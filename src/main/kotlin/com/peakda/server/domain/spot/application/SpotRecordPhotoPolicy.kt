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

    /**
     * 프런트가 화면 크기에 맞는 이미지를 직접 고를 수 있도록 세 벌을 만든다.
     * 카드 썸네일(80~256px), 피드 상세(430~1080px), 공유 카드·원본(1200px 이상) 순서다.
     */
    val VARIANTS: List<ImageVariant> = listOf(
        ImageVariant(name = "thumbnail", width = 256, height = 256, mode = ResizeMode.CROP, format = ImageFormat.JPEG),
        ImageVariant(name = "medium", width = 1080, height = 1080, mode = ResizeMode.FIT, format = ImageFormat.JPEG),
        ImageVariant(name = "main", width = 1600, height = 1600, mode = ResizeMode.FIT, format = ImageFormat.JPEG),
    )

    const val MAIN_VARIANT: String = "main"
    const val THUMBNAIL_VARIANT: String = "thumbnail"

    /** 새로 업로드한 사진이 보유하는 variant 이름 목록. `spot_record_photos.variant_names` 에 그대로 저장한다. */
    val CURRENT_VARIANT_NAMES: String = VARIANTS.joinToString(",") { it.name }

    /** `variant_names` 가 비어 있는 과거 사진은 thumbnail(400) 과 main 두 벌만 갖고 있다. */
    private val LEGACY_VARIANT_NAMES: Set<String> = setOf(THUMBNAIL_VARIANT, MAIN_VARIANT)

    fun availableVariantNames(variantNames: String?): Set<String> {
        val names = variantNames
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return if (names.isEmpty()) LEGACY_VARIANT_NAMES else names.toSet()
    }

    /** main key(`{prefix}/main.jpg`) 에서 같은 사진의 다른 variant key 를 만든다. */
    fun variantKeyOf(mainKey: String, variant: ImageVariant): String =
        keyOf(mainKey.substringBeforeLast('/'), variant)

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
