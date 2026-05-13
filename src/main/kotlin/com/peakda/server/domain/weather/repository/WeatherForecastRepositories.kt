package com.peakda.server.domain.weather.repository

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import org.springframework.data.jpa.repository.JpaRepository

interface WeatherMidForecastRepository : JpaRepository<WeatherMidForecast, Long> {
    fun findByRegIdAndTmFc(regId: String, tmFc: String): WeatherMidForecast?
}

interface WeatherShortForecastRepository : JpaRepository<WeatherShortForecast, Long> {
    fun findByNxAndNyAndFcstDateAndFcstTimeAndCategory(
        nx: Int,
        ny: Int,
        fcstDate: String,
        fcstTime: String,
        category: String,
    ): WeatherShortForecast?
}
