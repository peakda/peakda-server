package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.entity.CongestionLinkStatus

/** 매칭 잡 한 번의 결과. 상태별 건수는 이번 실행에서 새로 판정한 행만 센다. */
data class CongestionLinkSummary(
    val evaluated: Int = 0,
    val skipped: Int = 0,
    val byStatus: Map<CongestionLinkStatus, Int> = emptyMap(),
) {
    operator fun plus(other: CongestionLinkSummary) = CongestionLinkSummary(
        evaluated = evaluated + other.evaluated,
        skipped = skipped + other.skipped,
        byStatus = (byStatus.keys + other.byStatus.keys).associateWith {
            (byStatus[it] ?: 0) + (other.byStatus[it] ?: 0)
        },
    )
}
