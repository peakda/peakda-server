package com.peakda.server.domain.attraction.repository

/** 이름 매칭 후보. 매칭에 필요한 컬럼만 읽는다. */
data class AttractionNameCandidate(
    val id: Long,
    val title: String,
)
