package com.peakda.server.infrastructure.scheduler.seasonal

import com.peakda.server.domain.seasonal.application.DailyTemperature
import com.peakda.server.domain.seasonal.application.GddSnapshot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BloomEstimateJobTest {
    private val today = LocalDate.of(2026, 7, 30)

    @Test
    fun `실측 마지막이 어제면 오늘부터 예보를 쓴다`() {
        val observed = listOf(temperature(today.minusDays(1)))

        val forecastStart = BloomEstimateJob.resolveForecastStart(observed, today)

        assertThat(forecastStart).isEqualTo(today)
    }

    @Test
    fun `실측이 며칠 밀려 있으면 그 다음날부터 예보를 써 공백을 메운다`() {
        val observed = listOf(temperature(today.minusDays(3)))

        val forecastStart = BloomEstimateJob.resolveForecastStart(observed, today)

        assertThat(forecastStart).isEqualTo(today.minusDays(2))
    }

    @Test
    fun `실측에 오늘이 이미 있으면 내일부터 예보를 쓴다`() {
        val observed = listOf(temperature(today))

        val forecastStart = BloomEstimateJob.resolveForecastStart(observed, today)

        assertThat(forecastStart).isEqualTo(today.plusDays(1))
    }

    @Test
    fun `실측이 비어 있으면 오늘부터 예보를 쓴다`() {
        val forecastStart = BloomEstimateJob.resolveForecastStart(emptyList(), today)

        assertThat(forecastStart).isEqualTo(today)
    }

    @Test
    fun `실측이 날짜 순서와 무관하게 들어와도 가장 늦은 관측일을 기준으로 한다`() {
        val observed = listOf(
            temperature(today.minusDays(1)),
            temperature(today.minusDays(5)),
            temperature(today.minusDays(3)),
        )

        val forecastStart = BloomEstimateJob.resolveForecastStart(observed, today)

        assertThat(forecastStart).isEqualTo(today)
    }

    @Test
    fun `매핑이 없는 명소는 기본 지점 스냅샷을 받는다`() {
        val defaultSnapshot = GddSnapshot(stationId = "108", accumulated = 72.5)

        val result = BloomEstimateJob.resolveGddByAttraction(
            attractionIds = listOf(1L, 2L),
            stationByAttraction = mapOf(2L to "184"),
            defaultStationId = "108",
            snapshotByStation = mapOf(
                "108" to defaultSnapshot,
                "184" to GddSnapshot(stationId = "184", accumulated = 105.0),
            ),
        )

        assertThat(result[1L]).isSameAs(defaultSnapshot)
        assertThat(result[2L]?.stationId).isEqualTo("184")
    }

    private fun temperature(observedOn: LocalDate) = DailyTemperature(
        observedOn = observedOn,
        avgTemperature = null,
        minTemperature = null,
        maxTemperature = null,
    )
}
