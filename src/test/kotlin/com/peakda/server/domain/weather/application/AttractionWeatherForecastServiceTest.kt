package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.entity.AttractionForecastArea
import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import com.peakda.server.domain.weather.repository.AttractionForecastAreaRepository
import com.peakda.server.domain.weather.repository.WeatherMidForecastRepository
import com.peakda.server.domain.weather.repository.WeatherShortForecastRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AttractionWeatherForecastServiceTest {
    private val areaRepository = mock(AttractionForecastAreaRepository::class.java)
    private val shortRepository = mock(WeatherShortForecastRepository::class.java)
    private val midRepository = mock(WeatherMidForecastRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-04-01T00:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = AttractionWeatherForecastService(areaRepository, shortRepository, midRepository, clock)
    private val today = LocalDate.of(2026, 4, 1)

    @Test
    fun `예보 구역 매핑이 없으면 null 이다`() {
        assertThat(service.findForecast(1, today, today.plusDays(6))).isNull()
    }

    @Test
    fun `단기예보로 낮 시간 대표 하늘·최대 강수확률·최저최고를 요약하고 연속 강수를 구간으로 묶는다`() {
        `when`(areaRepository.findByAttractionId(1)).thenReturn(AttractionForecastArea(1, 60, 127, "SEOUL"))
        `when`(shortRepository.findByGridXAndGridYAndForecastCategoryInAndForecastDateBetween(
            anyInt(), anyInt(), anyCollection(), anyString(), anyString(),
        )).thenReturn(
            listOf(
                short("20260401", "0900", "SKY", "1"),
                short("20260401", "1200", "SKY", "1"),
                short("20260401", "1500", "SKY", "3"),
                short("20260401", "1200", "POP", "20"),
                short("20260401", "0600", "TMN", "8.0"),
                short("20260401", "1500", "TMX", "19.0"),
                short("20260402", "0600", "PTY", "1"),
                short("20260402", "0700", "PTY", "1"),
                short("20260402", "0800", "PTY", "0"),
                short("20260402", "0700", "POP", "70"),
            ),
        )

        val forecast = service.findForecast(1, today, today.plusDays(6))!!

        val first = forecast.daily.first { it.date == today }
        assertThat(first.sky).isEqualTo(WeatherSky.CLEAR)
        assertThat(first.precipitationProbability).isEqualTo(20)
        assertThat(first.minTemperature).isEqualTo(8.0)
        assertThat(first.maxTemperature).isEqualTo(19.0)
        assertThat(forecast.daily.first { it.date == today.plusDays(1) }.sky).isEqualTo(WeatherSky.RAIN)
        assertThat(forecast.rainWindows).containsExactly(
            RainWindow(LocalDateTime.of(2026, 4, 2, 6, 0), LocalDateTime.of(2026, 4, 2, 8, 0), WeatherSky.RAIN),
        )
    }

    @Test
    fun `3일 이후는 중기예보로 채우고 오전·오후 중 나쁜 쪽을 쓴다`() {
        `when`(areaRepository.findByAttractionId(1)).thenReturn(AttractionForecastArea(1, 60, 127, "SEOUL"))
        `when`(midRepository.findFirstByRegionCodeOrderByAnnounceTimeDesc("SEOUL")).thenReturn(
            WeatherMidForecast(regionCode = "SEOUL", announceTime = "202604010600").apply {
                weatherDay3Am = "맑음"
                weatherDay3Pm = "구름많고 비"
                rainProbabilityDay3Am = 10
                rainProbabilityDay3Pm = 60
                temperatureMinDay3 = 9
                temperatureMaxDay3 = 21
            },
        )

        val forecast = service.findForecast(1, today, today.plusDays(6))!!

        val day3 = forecast.daily.single { it.date == today.plusDays(3) }
        assertThat(day3.sky).isEqualTo(WeatherSky.RAIN)
        assertThat(day3.precipitationProbability).isEqualTo(60)
        assertThat(day3.maxTemperature).isEqualTo(21.0)
    }

    @Test
    fun `밤 시간만 있는 단기예보 날은 비어 있는 항목을 중기예보로 채운다`() {
        `when`(areaRepository.findByAttractionId(1)).thenReturn(AttractionForecastArea(1, 60, 127, "SEOUL"))
        `when`(shortRepository.findByGridXAndGridYAndForecastCategoryInAndForecastDateBetween(
            anyInt(), anyInt(), anyCollection(), anyString(), anyString(),
        )).thenReturn(listOf(short("20260404", "0000", "TMP", "7.0")))
        `when`(midRepository.findFirstByRegionCodeOrderByAnnounceTimeDesc("SEOUL")).thenReturn(
            WeatherMidForecast(regionCode = "SEOUL", announceTime = "202604010600").apply {
                weatherDay3Am = "맑음"
                weatherDay3Pm = "맑음"
                rainProbabilityDay3Am = 10
                rainProbabilityDay3Pm = 20
                temperatureMinDay3 = 9
                temperatureMaxDay3 = 21
            },
        )

        val day = service.findForecast(1, today, today.plusDays(6))!!.daily.single { it.date == today.plusDays(3) }

        assertThat(day.sky).isEqualTo(WeatherSky.CLEAR)
        assertThat(day.precipitationProbability).isEqualTo(20)
        assertThat(day.minTemperature).isEqualTo(9.0)
        assertThat(day.maxTemperature).isEqualTo(21.0)
    }

    private fun short(date: String, time: String, category: String, value: String) = WeatherShortForecast(
        gridX = 60,
        gridY = 127,
        announceDate = "20260401",
        announceTime = "0500",
        forecastDate = date,
        forecastTime = time,
        forecastCategory = category,
        forecastValue = value,
    )
}
