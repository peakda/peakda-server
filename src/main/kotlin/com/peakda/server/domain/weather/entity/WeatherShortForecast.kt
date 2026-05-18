package com.peakda.server.domain.weather.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "weather_short_forecasts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_weather_short_forecasts_grid_forecast",
            columnNames = ["grid_x", "grid_y", "forecast_date", "forecast_time", "forecast_category"],
        ),
    ],
)
class WeatherShortForecast(
    @Column(name = "grid_x", nullable = false)
    val gridX: Int,

    @Column(name = "grid_y", nullable = false)
    val gridY: Int,

    @Column(name = "announce_date", nullable = false, columnDefinition = "TEXT")
    var announceDate: String,

    @Column(name = "announce_time", nullable = false, columnDefinition = "TEXT")
    var announceTime: String,

    @Column(name = "forecast_date", nullable = false, columnDefinition = "TEXT")
    val forecastDate: String,

    @Column(name = "forecast_time", nullable = false, columnDefinition = "TEXT")
    val forecastTime: String,

    @Column(name = "forecast_category", nullable = false, columnDefinition = "TEXT")
    val forecastCategory: String,

    @Column(name = "forecast_value", nullable = false, columnDefinition = "TEXT")
    var forecastValue: String,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
