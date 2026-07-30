package com.peakda.server.domain.seasonal.application

import java.time.LocalDate

object GddProjector {
    /**
     * [accumulated] 에 예보 일평균을 하루씩 더해 [threshold] 를 처음 넘는 날짜를 찾는다.
     *
     * 이미 넘어선 상태이거나 예보 범위 안에서 도달하지 못하면 null 이다.
     * 과거 도달일은 예보로 알 수 없어 추정하지 않는다.
     */
    fun projectThresholdDate(
        accumulated: Double,
        forecasts: List<DailyTemperature>,
        tBase: Double,
        threshold: Double,
    ): LocalDate? {
        if (accumulated >= threshold) return null

        var projected = accumulated
        for (forecast in forecasts.sortedBy(DailyTemperature::observedOn)) {
            val effectiveAverage = forecast.effectiveAverage ?: continue
            projected += maxOf(0.0, effectiveAverage - tBase)
            if (projected >= threshold) return forecast.observedOn
        }
        return null
    }
}
