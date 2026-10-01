package com.peakda.server.domain.visittiming.application

/** 집중률(관광지 자체 기준 상대 지수)을 나눈 혼잡 등급. 방문객 절대 인원이 아니다. */
enum class CongestionLevel {
    QUIET,
    NORMAL,
    BUSY,
    VERY_BUSY,
    ;

    companion object {
        fun of(rate: Double, thresholds: VisitTimingProperties.CongestionThresholds): CongestionLevel = when {
            rate < thresholds.quietBelow -> QUIET
            rate < thresholds.normalBelow -> NORMAL
            rate < thresholds.busyBelow -> BUSY
            else -> VERY_BUSY
        }
    }
}
