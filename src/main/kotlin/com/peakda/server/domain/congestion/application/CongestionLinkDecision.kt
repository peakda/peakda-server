package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.entity.CongestionMatchType

/** 관광지명 하나에 대한 매칭 판정 결과. */
data class CongestionLinkDecision(
    val status: CongestionLinkStatus,
    val matchType: CongestionMatchType?,
    val attractionId: Long?,
    val candidateIds: List<Long>,
)
