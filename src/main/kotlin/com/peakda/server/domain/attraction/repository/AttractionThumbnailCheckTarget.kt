package com.peakda.server.domain.attraction.repository

/** 썸네일 URL 을 확인할 명소. */
data class AttractionThumbnailCheckTarget(
    val attractionId: Long,
    val thumbnailImageUrl: String,
)
