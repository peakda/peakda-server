package com.peakda.server.domain.curation.application

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.image.ImageException
import com.peakda.server.common.image.ImageFormat
import com.peakda.server.common.image.ImageVariant
import com.peakda.server.common.image.ResizeMode
import org.springframework.web.multipart.MultipartFile
import java.time.YearMonth

object CurationImagePolicy {
    /** Figma 에서 내보낸 PNG 원본(4096px, 25MB 안팎)을 그대로 올려도 서버가 줄여 저장하도록 넉넉히 받는다. */
    const val MAX_FILE_SIZE_BYTES: Long = 30L * 1024 * 1024
    val ALLOWED_MIME_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp")

    private const val MAIN_VARIANT: String = "main"
    private const val QUALITY: Double = 0.8

    /**
     * 용도별 저장 크기. 원본이 기준보다 작으면 키우지 않는다.
     *
     * 화면 카드 폭은 430px 이다. 사진은 2배 밀도 기준 가로 860px 이면 충분하고,
     * 히어로는 이미지에 글자가 얹혀 있어 기존 상한(1600×900)까지 해상도를 유지한다.
     */
    fun variantOf(usage: CurationImageUsage): ImageVariant =
        when (usage) {
            CurationImageUsage.HERO -> webp(width = 1600, height = 900)
            CurationImageUsage.PHOTO -> webp(width = 860, height = 1720)
        }

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

    private fun webp(width: Int, height: Int): ImageVariant =
        ImageVariant(
            name = MAIN_VARIANT,
            width = width,
            height = height,
            mode = ResizeMode.FIT,
            format = ImageFormat.WEBP,
            quality = QUALITY,
            upscale = false,
        )
}
