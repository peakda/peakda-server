package com.peakda.server.domain.weather.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WeatherSkyTest {
    @Test
    fun `중기예보 문구를 하늘 상태로 바꾼다`() {
        assertThat(WeatherSky.fromMidForecastText("맑음")).isEqualTo(WeatherSky.CLEAR)
        assertThat(WeatherSky.fromMidForecastText("구름많음")).isEqualTo(WeatherSky.PARTLY_CLOUDY)
        assertThat(WeatherSky.fromMidForecastText("흐림")).isEqualTo(WeatherSky.CLOUDY)
        assertThat(WeatherSky.fromMidForecastText("구름많고 비")).isEqualTo(WeatherSky.RAIN)
        assertThat(WeatherSky.fromMidForecastText("흐리고 비/눈")).isEqualTo(WeatherSky.RAIN_SNOW)
        assertThat(WeatherSky.fromMidForecastText("구름많고 소나기")).isEqualTo(WeatherSky.SHOWER)
        assertThat(WeatherSky.fromMidForecastText("흐리고 눈")).isEqualTo(WeatherSky.SNOW)
        assertThat(WeatherSky.fromMidForecastText(null)).isNull()
    }

    @Test
    fun `단기예보 SKY·PTY 코드를 하늘 상태로 바꾼다`() {
        assertThat(WeatherSky.fromShortSkyCode("1")).isEqualTo(WeatherSky.CLEAR)
        assertThat(WeatherSky.fromShortSkyCode("4")).isEqualTo(WeatherSky.CLOUDY)
        assertThat(WeatherSky.fromPrecipitationCode("0")).isNull()
        assertThat(WeatherSky.fromPrecipitationCode("1")).isEqualTo(WeatherSky.RAIN)
        assertThat(WeatherSky.fromPrecipitationCode("4")).isEqualTo(WeatherSky.SHOWER)
    }
}
