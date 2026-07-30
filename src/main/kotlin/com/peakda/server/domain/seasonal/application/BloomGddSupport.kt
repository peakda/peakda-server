package com.peakda.server.domain.seasonal.application

import java.time.DateTimeException
import java.time.LocalDate

internal fun resolveAccumulationStart(year: Int, month: Int, day: Int): LocalDate {
    return try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        // 설정 오타가 있어도 배치와 캘린더가 같은 보수적 기준일로 누적을 시작해야 한다.
        LocalDate.ofYearDay(year, 1)
    }
}

internal fun resolveForecastStart(
    observed: List<DailyTemperature>,
    today: LocalDate,
): LocalDate = observed.maxOfOrNull { it.observedOn }?.plusDays(1) ?: today

internal fun accumulateByDate(
    baseAccumulated: Double,
    forecasts: List<DailyTemperature>,
    tBase: Double,
    dates: List<LocalDate>,
): Map<LocalDate, Double> {
    if (forecasts.isEmpty() || dates.isEmpty()) return emptyMap()

    val sortedForecasts = forecasts.sortedBy(DailyTemperature::observedOn)
    val lastForecastDate = sortedForecasts.last().observedOn
    var accumulated = baseAccumulated
    var forecastIndex = 0
    return dates.sorted()
        .filter { date -> !date.isAfter(lastForecastDate) }
        .associateWith { date ->
            while (
                forecastIndex < sortedForecasts.size &&
                !sortedForecasts[forecastIndex].observedOn.isAfter(date)
            ) {
                accumulated += sortedForecasts[forecastIndex].effectiveAverage
                    ?.let { effectiveAverage -> maxOf(0.0, effectiveAverage - tBase) }
                    ?: 0.0
                forecastIndex++
            }
            accumulated
        }
}
