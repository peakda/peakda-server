package com.peakda.server.domain.seasonal.application.estimator

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 달력 추정기(신호 C) 튜닝값. 운영 중 yml 로 조정한다.
 */
@ConfigurationProperties(prefix = "peakda.timing.calendar")
data class CalendarEstimatorProperties(
    /** 항상 가용한 fallback 신호이므로 낮게 둔다. */
    val baseConfidence: Double = 0.4,
    /** 절정 시작 며칠 전부터 STARTED 로 볼지. */
    val earlyWindowDays: Long = 7,
    /** 절정 종료 며칠 후까지 ENDED 로 볼지. */
    val lateWindowDays: Long = 7,
)
