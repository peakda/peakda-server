package com.peakda.server.domain.seasonal.application.estimator

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "peakda.timing.observation")
data class ObservationEstimatorProperties(
    val enabled: Boolean = true,
    /**
     * 기상청 직접 관측이라 추정 신호보다 높지만, 같은 권역의 다른 장소 관측이므로
     * 명소 자체의 관측은 아니다. 축제(0.9)와 같은 수준으로 둔다.
     */
    val baseConfidence: Double = 0.9,
    /** 만발일로부터 절정으로 볼 일수. */
    val peakDurationDays: Long = 7,
)
