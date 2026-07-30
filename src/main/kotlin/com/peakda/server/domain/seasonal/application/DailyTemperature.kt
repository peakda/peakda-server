package com.peakda.server.domain.seasonal.application

import java.time.LocalDate

/**
 * GDD 누적에 쓰는 하루치 기온.
 *
 * 수집 단계에서는 원천 관측을 왜곡하지 않고, 누적에 필요한 결측 보정만 이 타입에서 수행한다.
 */
data class DailyTemperature(
    val observedOn: LocalDate,
    val avgTemperature: Double?,
    val minTemperature: Double?,
    val maxTemperature: Double?,
) {
    /**
     * 누적에 쓸 일평균기온. 관측 결측 시 최저·최고의 중간값으로 대체하고,
     * 그마저 없으면 null 이라 그 날은 누적에서 제외된다.
     */
    val effectiveAverage: Double?
        get() = avgTemperature ?: if (minTemperature != null && maxTemperature != null) {
            (minTemperature + maxTemperature) / 2.0
        } else {
            null
        }
}
