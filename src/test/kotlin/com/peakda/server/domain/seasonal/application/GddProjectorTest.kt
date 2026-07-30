package com.peakda.server.domain.seasonal.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GddProjectorTest {

    @Test
    fun `예보를 누적해 임계치를 처음 넘는 날짜를 돌려준다`() {
        val forecasts = listOf(
            temperature(day = 2, average = 9.0),
            temperature(day = 1, average = 8.0),
        )

        val projected = GddProjector.projectThresholdDate(
            accumulated = 5.0,
            forecasts = forecasts,
            tBase = 5.0,
            threshold = 10.0,
        )

        assertThat(projected).isEqualTo(LocalDate.of(2026, 4, 2))
    }

    @Test
    fun `이미 임계치를 넘어선 상태면 null 이다`() {
        val projected = GddProjector.projectThresholdDate(
            accumulated = 10.0,
            forecasts = listOf(temperature(day = 1, average = 20.0)),
            tBase = 5.0,
            threshold = 10.0,
        )

        assertThat(projected).isNull()
    }

    @Test
    fun `예보 범위 안에서 도달하지 못하면 null 이다`() {
        val projected = GddProjector.projectThresholdDate(
            accumulated = 0.0,
            forecasts = listOf(temperature(day = 1, average = 6.0)),
            tBase = 5.0,
            threshold = 10.0,
        )

        assertThat(projected).isNull()
    }

    @Test
    fun `기준온도 이하인 날은 누적에 기여하지 않아 도달일이 뒤로 밀린다`() {
        val forecasts = listOf(
            temperature(day = 1, average = 4.0),
            temperature(day = 2, average = 9.0),
        )

        val projected = GddProjector.projectThresholdDate(
            accumulated = 0.0,
            forecasts = forecasts,
            tBase = 5.0,
            threshold = 4.0,
        )

        assertThat(projected).isEqualTo(LocalDate.of(2026, 4, 2))
    }

    @Test
    fun `기온이 결측인 날은 건너뛴다`() {
        val forecasts = listOf(
            temperature(day = 1, average = null),
            temperature(day = 2, average = 10.0),
        )

        val projected = GddProjector.projectThresholdDate(
            accumulated = 0.0,
            forecasts = forecasts,
            tBase = 5.0,
            threshold = 5.0,
        )

        assertThat(projected).isEqualTo(LocalDate.of(2026, 4, 2))
    }

    @Test
    fun `예보가 비어 있으면 null 이다`() {
        val projected = GddProjector.projectThresholdDate(
            accumulated = 0.0,
            forecasts = emptyList(),
            tBase = 5.0,
            threshold = 5.0,
        )

        assertThat(projected).isNull()
    }

    private fun temperature(day: Int, average: Double?) = DailyTemperature(
        observedOn = LocalDate.of(2026, 4, day),
        avgTemperature = average,
        minTemperature = null,
        maxTemperature = null,
    )
}
