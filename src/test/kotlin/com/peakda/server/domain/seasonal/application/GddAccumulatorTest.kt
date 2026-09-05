package com.peakda.server.domain.seasonal.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GddAccumulatorTest {

    @Test
    fun `기준온도 초과분만 누적한다`() {
        val temperatures = listOf(
            temperature(day = 1, average = 6.0),
            temperature(day = 2, average = 10.0),
        )

        assertThat(GddAccumulator.accumulate(temperatures, tBase = 5.0)).isEqualTo(6.0)
    }

    @Test
    fun `기준온도 이하인 날은 음수로 누적을 깎지 않는다`() {
        val temperatures = listOf(
            temperature(day = 1, average = 4.0),
            temperature(day = 2, average = 5.0),
        )

        assertThat(GddAccumulator.accumulate(temperatures, tBase = 5.0)).isEqualTo(0.0)
    }

    @Test
    fun `일평균기온 결측 시 최저 최고 중간값으로 누적한다`() {
        val temperatures = listOf(
            temperature(day = 1, average = null, minimum = 4.0, maximum = 10.0),
        )

        assertThat(GddAccumulator.accumulate(temperatures, tBase = 5.0)).isEqualTo(2.0)
    }

    @Test
    fun `기온이 전부 결측인 날은 건너뛴다`() {
        val temperatures = listOf(
            temperature(day = 1, average = null, minimum = null, maximum = null),
            temperature(day = 2, average = 8.0),
        )

        assertThat(GddAccumulator.accumulate(temperatures, tBase = 5.0)).isEqualTo(3.0)
    }

    @Test
    fun `빈 목록이면 0 이다`() {
        assertThat(GddAccumulator.accumulate(emptyList(), tBase = 5.0)).isEqualTo(0.0)
    }

    private fun temperature(
        day: Int,
        average: Double?,
        minimum: Double? = null,
        maximum: Double? = null,
    ) = DailyTemperature(
        observedOn = LocalDate.of(2026, 1, day),
        avgTemperature = average,
        minTemperature = minimum,
        maxTemperature = maximum,
    )
}
