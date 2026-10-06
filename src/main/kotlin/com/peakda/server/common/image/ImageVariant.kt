package com.peakda.server.common.image

data class ImageVariant(
    val name: String,
    val width: Int,
    val height: Int,
    val mode: ResizeMode = ResizeMode.CROP,
    val format: ImageFormat = ImageFormat.JPEG,
    val quality: Double = 0.85,
    /** false 면 원본이 width×height 안에 들어올 때 키우지 않고 원본 크기로 저장한다. FIT 에서만 의미가 있다. */
    val upscale: Boolean = true,
) {
    init {
        require(width > 0 && height > 0) { "width/height must be positive" }
        require(quality in 0.0..1.0) { "quality must be between 0.0 and 1.0" }
        require(upscale || mode == ResizeMode.FIT) { "upscale=false is only supported in FIT mode" }
    }
}

enum class ResizeMode {
    CROP,
    FIT,
}
