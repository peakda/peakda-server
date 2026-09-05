package com.peakda.server.domain.seasonal.application.estimator

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 축제 추정기(신호 B) 튜닝값. 운영 중 yml 로 조정한다.
 */
@ConfigurationProperties(prefix = "peakda.timing.festival")
data class FestivalEstimatorProperties(
    /** 명소 ↔ 축제 근접 반경(km). */
    val proximityRadiusKm: Double = 5.0,
    /** 축제 시작 며칠 전부터 STARTED 로 볼지. */
    val earlyWindowDays: Long = 3,
    /** 축제 종료 며칠 후까지 ENDED 로 볼지. */
    val lateWindowDays: Long = 3,
    /** 축제 기간 중(PEAK) 신뢰도. */
    val peakConfidence: Double = 0.9,
    /** 축제 시작 직전(STARTED) 신뢰도. */
    val startedConfidence: Double = 0.8,
    /** 축제 종료 직후(ENDED) 신뢰도. */
    val endedConfidence: Double = 0.7,
)
