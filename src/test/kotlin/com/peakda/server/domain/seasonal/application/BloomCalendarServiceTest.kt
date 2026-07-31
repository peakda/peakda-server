package com.peakda.server.domain.seasonal.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BloomCalendarServiceTest {

    @Test
    fun `예보를 하루씩 더해 날짜별 누적이 증가한다`() {
        val accumulatedByDate = accumulateByDate(
            baseAccumulated = 10.0,
            forecasts = listOf(
                temperature(day = 1, average = 8.0),
                temperature(day = 2, average = 10.0),
            ),
            tBase = 5.0,
            dates = listOf(date(day = 1), date(day = 2)),
        )

        assertThat(accumulatedByDate).containsExactlyEntriesOf(
            linkedMapOf(
                date(day = 1) to 13.0,
                date(day = 2) to 18.0,
            ),
        )
    }

    @Test
    fun `예보 범위를 넘는 날짜는 결과에 없다`() {
        val accumulatedByDate = accumulateByDate(
            baseAccumulated = 10.0,
            forecasts = listOf(temperature(day = 1, average = 8.0)),
            tBase = 5.0,
            dates = listOf(date(day = 1), date(day = 2)),
        )

        assertThat(accumulatedByDate).containsOnlyKeys(date(day = 1))
    }

    private fun temperature(
        day: Int,
        average: Double?,
        minimum: Double? = null,
        maximum: Double? = null,
    ) = DailyTemperature(
        observedOn = date(day),
        avgTemperature = average,
        minTemperature = minimum,
        maxTemperature = maximum,
    )

    private fun date(day: Int): LocalDate = LocalDate.of(2026, 3, day)
}
