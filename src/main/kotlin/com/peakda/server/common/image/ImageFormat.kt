package com.peakda.server.common.image

enum class ImageFormat(
    val extension: String,
    val mimeType: String,
) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp"),
    ;

    companion object {
        fun fromMimeType(mimeType: String?): ImageFormat? = entries.firstOrNull { it.mimeType.equals(mimeType, ignoreCase = true) }
    }
}
