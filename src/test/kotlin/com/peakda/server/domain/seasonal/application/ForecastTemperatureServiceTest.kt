package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import com.peakda.server.domain.weather.repository.WeatherMidForecastRepository
import com.peakda.server.domain.weather.repository.WeatherShortForecastRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate

class ForecastTemperatureServiceTest {

    private val shortForecastRepository = mock(WeatherShortForecastRepository::class.java)
    private val midForecastRepository = mock(WeatherMidForecastRepository::class.java)
    private val service = ForecastTemperatureService(shortForecastRepository, midForecastRepository)

    @Test
    fun `단기예보 TMN 과 TMX 를 같은 날짜의 최저 최고로 합친다`() {
        stubShortForecasts(
            shortForecast("20260302", "TMN", "2.5"),
            shortForecast("20260302", "TMX", "12.5"),
        )
        stubMidForecast(null)

        val temperatures = load()

        assertThat(temperatures).hasSize(1)
        assertThat(temperatures.first()).isEqualTo(
            DailyTemperature(
                observedOn = LocalDate.of(2026, 3, 2),
                avgTemperature = null,
                minTemperature = 2.5,
                maxTemperature = 12.5,
            ),
        )
    }

    @Test
    fun `중기예보 dayN 을 발표일 기준 날짜로 환산한다`() {
        stubShortForecasts()
        stubMidForecast(
            midForecast(
                announceTime = "202603010600",
                temperatureMinDay3 = 3,
                temperatureMaxDay3 = 13,
            ),
        )

        val temperatures = load()

        assertThat(temperatures.first()).isEqualTo(
            DailyTemperature(
                observedOn = LocalDate.of(2026, 3, 4),
                avgTemperature = null,
                minTemperature = 3.0,
                maxTemperature = 13.0,
            ),
        )
    }

    @Test
    fun `단기와 중기가 겹치는 날짜에는 단기예보가 우선한다`() {
        stubShortForecasts(
            shortForecast("20260304", "TMN", "1.0"),
            shortForecast("20260304", "TMX", "11.0"),
        )
        stubMidForecast(
            midForecast(
                announceTime = "202603010600",
                temperatureMinDay3 = 5,
                temperatureMaxDay3 = 15,
            ),
        )

        val temperature = load().single()

        assertThat(temperature.minTemperature).isEqualTo(1.0)
        assertThat(temperature.maxTemperature).isEqualTo(11.0)
    }

    @Test
    fun `결과를 날짜 오름차순으로 돌려준다`() {
        stubShortForecasts(
            shortForecast("20260303", "TMN", "3.0"),
            shortForecast("20260301", "TMN", "1.0"),
        )
        stubMidForecast(
            midForecast(
                announceTime = "202603010600",
                temperatureMinDay3 = 4,
                temperatureMaxDay3 = 14,
            ),
        )

        val temperatures = load()

        assertThat(temperatures.map(DailyTemperature::observedOn)).containsExactly(
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 3),
            LocalDate.of(2026, 3, 4),
        )
    }

    private fun load() = service.loadForecastTemperatures(
        gridX = GRID_X,
        gridY = GRID_Y,
        midRegionCode = MID_REGION_CODE,
        from = FROM,
        to = TO,
    )

    private fun stubShortForecasts(vararg forecasts: WeatherShortForecast) {
        `when`(
            shortForecastRepository.findByGridXAndGridYAndForecastCategoryInAndForecastDateBetween(
                GRID_X,
                GRID_Y,
                listOf("TMN", "TMX"),
                "20260301",
                "20260310",
            ),
        ).thenReturn(forecasts.toList())
    }

    private fun stubMidForecast(forecast: WeatherMidForecast?) {
        `when`(midForecastRepository.findFirstByRegionCodeOrderByAnnounceTimeDesc(MID_REGION_CODE))
            .thenReturn(forecast)
    }

    private fun shortForecast(
        forecastDate: String,
        category: String,
        value: String,
    ) = WeatherShortForecast(
        gridX = GRID_X,
        gridY = GRID_Y,
        announceDate = "20260301",
        announceTime = "0200",
        forecastDate = forecastDate,
        forecastTime = "0600",
        forecastCategory = category,
        forecastValue = value,
    )

    private fun midForecast(
        announceTime: String,
        temperatureMinDay3: Int,
        temperatureMaxDay3: Int,
    ) = WeatherMidForecast(
        regionCode = MID_REGION_CODE,
        announceTime = announceTime,
        temperatureMinDay3 = temperatureMinDay3,
        temperatureMaxDay3 = temperatureMaxDay3,
    )

    companion object {
        private const val GRID_X = 60
        private const val GRID_Y = 127
        private const val MID_REGION_CODE = "SEOUL"
        private val FROM = LocalDate.of(2026, 3, 1)
        private val TO = LocalDate.of(2026, 3, 10)
    }
}
