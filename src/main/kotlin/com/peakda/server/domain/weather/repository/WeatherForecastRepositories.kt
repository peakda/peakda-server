package com.peakda.server.domain.weather.repository

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import org.springframework.data.jpa.repository.JpaRepository

interface WeatherMidForecastRepository : JpaRepository<WeatherMidForecast, Long> {
    fun findByRegionCodeAndAnnounceTime(regionCode: String, announceTime: String): WeatherMidForecast?
}

interface WeatherShortForecastRepository : JpaRepository<WeatherShortForecast, Long> {
    fun findByGridXAndGridYAndForecastDateAndForecastTimeAndForecastCategory(
        gridX: Int,
        gridY: Int,
        forecastDate: String,
        forecastTime: String,
        forecastCategory: String,
    ): WeatherShortForecast?
}
