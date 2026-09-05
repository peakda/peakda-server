package com.peakda.server.common.image

data class ImageVariant(
    val name: String,
    val width: Int,
    val height: Int,
    val mode: ResizeMode = ResizeMode.CROP,
    val format: ImageFormat = ImageFormat.JPEG,
    val quality: Double = 0.85,
) {
    init {
        require(width > 0 && height > 0) { "width/height must be positive" }
        require(quality in 0.0..1.0) { "quality must be between 0.0 and 1.0" }
    }
}

enum class ResizeMode {
    CROP,
    FIT,
}
