package com.peakda.server.common.image

enum class ImageFormat(
    val extension: String,
    val mimeType: String,
    /** ImageIO writer 에 넘길 압축 방식. null 이면 writer 기본값을 쓴다. WebP 는 지정하지 않으면 무손실로 저장될 수 있다. */
    val compressionType: String? = null,
) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp", compressionType = "Lossy"),
    ;

    companion object {
        fun fromMimeType(mimeType: String?): ImageFormat? = entries.firstOrNull { it.mimeType.equals(mimeType, ignoreCase = true) }
    }
}
