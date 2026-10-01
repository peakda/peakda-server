package com.peakda.server.domain.congestion.repository

/** 집중률 관광지 자연키 (기준일자 제외). */
data class CongestionAttractionKey(
    val areaCode: String,
    val sigunguCode: String,
    val touristAttractionName: String,
)
