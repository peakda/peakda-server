package com.peakda.server.domain.attraction.repository

/** 운영 정보를 받을 명소. 관광공사 상세 조회와 재수집 판단에 필요한 컬럼만 읽는다. */
data class AttractionOperatingInfoTarget(
    val attractionId: Long,
    val tourApiContentId: String,
    val contentTypeCode: String,
    val externalModifiedAt: String?,
)
