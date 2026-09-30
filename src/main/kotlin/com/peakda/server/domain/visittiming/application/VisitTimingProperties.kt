package com.peakda.server.domain.visittiming.application

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 방문 타이밍 판단 기준.
 *
 * 혼잡 구간 기본값은 2026-09 운영 집중률 분포(p25 36 / p50 55 / p75 76 / p90 91)의 사분위 근처로 잡았다.
 */
@ConfigurationProperties(prefix = "peakda.visit-timing")
data class VisitTimingProperties(
    /** 오늘 포함 며칠을 보여주고 추천 후보로 삼을지. */
    val forecastDays: Long = 7,
    val festivalRadiusMeters: Double = 5_000.0,
    /** 오늘 이후 며칠 안에 시작하는 축제까지 보여줄지. */
    val festivalLookaheadDays: Long = 14,
    val maxFestivals: Int = 2,
    /** 낮 강수확률이 이 값 이상이면 추천일에서 뺀다. */
    val rainyProbabilityThreshold: Int = 60,
    /** 낮 강수확률이 이 값 미만이고 맑거나 구름 조금이면 "날씨 좋음" 이유를 붙인다. */
    val clearProbabilityThreshold: Int = 30,
    val congestion: CongestionThresholds = CongestionThresholds(),
) {
    data class CongestionThresholds(
        val quietBelow: Double = 35.0,
        val normalBelow: Double = 65.0,
        val busyBelow: Double = 85.0,
    )
}
