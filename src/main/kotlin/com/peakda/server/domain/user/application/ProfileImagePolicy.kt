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
    const val THUMBNAIL_VARIANT = "thumbnail"

    private const val ROOT = "profile-images"

    /**
     * 업로드마다 새 prefix 를 만든다.
     *
     * 주소가 고정이면 사용자가 사진을 바꿔도 CDN 이 옛 이미지를 계속 내려주고,
     * userId 만 알면 남의 프로필 이미지 주소를 추측할 수 있다. 임의 문자열이 둘 다 막는다.
     */
    fun prefixOf(userId: Long, uuid: String): String = "$ROOT/$userId/$uuid"

    fun keyOf(prefix: String, variant: ImageVariant): String =
        "$prefix/${variant.name}.${variant.format.extension}"

    /**
     * 저장된 main key 에서 같은 이미지의 다른 variant key 를 만든다.
     *
     * 가입 중 임시 업로드(`temp/signup/...`)와 prefix 가 고정이던 과거 업로드도 같은 규칙으로 처리된다.
     */
    fun variantKeyOf(mainKey: String, variant: ImageVariant): String =
        keyOf(mainKey.substringBeforeLast('/'), variant)

    /** 우리가 이 사용자 몫으로 올린 이미지인지. 외부 OAuth 가 준 URL 과 구분한다. */
    fun isManagedKey(userId: Long, value: String): Boolean = value.startsWith("$ROOT/$userId/")

    fun validate(file: MultipartFile) {
        if (file.isEmpty) throw ImageException(ErrorCode.IMAGE_REQUIRED)
        if (file.size > MAX_FILE_SIZE_BYTES) throw ImageException(ErrorCode.IMAGE_SIZE_EXCEEDED)
        val contentType = file.contentType?.lowercase()
        if (contentType !in ALLOWED_MIME_TYPES) throw ImageException(ErrorCode.INVALID_IMAGE_FORMAT)
    }
}
