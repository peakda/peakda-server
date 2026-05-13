package com.peakda.server.domain.weather.entity

import com.peakda.server.global.persistence.BaseTimeEntity
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
            name = "uk_weather_short_forecasts_grid_fcst",
            columnNames = ["nx", "ny", "fcst_date", "fcst_time", "category"],
        ),
    ],
)
class WeatherShortForecast(
    @Column(name = "nx", nullable = false)
    val nx: Int,

    @Column(name = "ny", nullable = false)
    val ny: Int,

    @Column(name = "base_date", nullable = false, columnDefinition = "TEXT")
    var baseDate: String,

    @Column(name = "base_time", nullable = false, columnDefinition = "TEXT")
    var baseTime: String,

    @Column(name = "fcst_date", nullable = false, columnDefinition = "TEXT")
    val fcstDate: String,

    @Column(name = "fcst_time", nullable = false, columnDefinition = "TEXT")
    val fcstTime: String,

    @Column(name = "category", nullable = false, columnDefinition = "TEXT")
    val category: String,

    @Column(name = "fcst_value", nullable = false, columnDefinition = "TEXT")
    var fcstValue: String,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
