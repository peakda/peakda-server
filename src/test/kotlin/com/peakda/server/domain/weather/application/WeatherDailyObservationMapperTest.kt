package com.peakda.server.domain.weather.application

import com.peakda.server.infrastructure.external.kma.asosdaly.response.AsosDalyItem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class WeatherDailyObservationMapperTest {
    @Test
    fun `tm이 yyyy-MM-dd이면 파싱된다`() {
        val command = item(tm = "2026-07-29").toUpsertCommand()

        assertThat(command?.observedOn).isEqualTo(LocalDate.of(2026, 7, 29))
    }

    @Test
    fun `tm이 yyyyMMdd이면 파싱된다`() {
        val command = item(tm = "20260729").toUpsertCommand()

        assertThat(command?.observedOn).isEqualTo(LocalDate.of(2026, 7, 29))
    }

    @Test
    fun `tm이 파싱 불가이면 null을 반환한다`() {
        val command = item(tm = "2026/07/29").toUpsertCommand()

        assertThat(command).isNull()
    }

    @Test
    fun `기온이 빈 문자열이면 보정하지 않고 null로 매핑한다`() {
        val command = item(
            tm = "2026-07-29",
            avgTa = "",
            minTa = "20.0",
            maxTa = "30.0",
        ).toUpsertCommand()

        assertThat(command?.avgTemperature).isNull()
        assertThat(command?.minTemperature).isEqualTo(20.0)
        assertThat(command?.maxTemperature).isEqualTo(30.0)
    }

    private fun item(
        tm: String,
        avgTa: String = "25.0",
        minTa: String = "20.0",
        maxTa: String = "30.0",
    ) = AsosDalyItem(
        tm = tm,
        stnId = "108",
        stnNm = "서울",
        avgTa = avgTa,
        minTa = minTa,
        maxTa = maxTa,
    )
}
