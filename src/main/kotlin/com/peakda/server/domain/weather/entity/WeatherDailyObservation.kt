package com.peakda.server.domain.weather.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/** ASOS 관측 결측을 원본 그대로 보존하기 위해 기온 값은 nullable 이다. */
@Entity
@Table(
    name = "weather_daily_observations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_weather_daily_observations_station_date",
            columnNames = ["station_id", "observed_on"],
        ),
    ],
    indexes = [
        Index(name = "ix_weather_daily_observations_observed_on", columnList = "observed_on"),
    ],
)
class WeatherDailyObservation(
    @Column(name = "station_id", nullable = false, columnDefinition = "TEXT")
    val stationId: String,

    @Column(name = "observed_on", nullable = false)
    val observedOn: LocalDate,

    @Column(name = "station_name", columnDefinition = "TEXT")
    var stationName: String? = null,

    @Column(name = "avg_temperature")
    var avgTemperature: Double? = null,

    @Column(name = "min_temperature")
    var minTemperature: Double? = null,

    @Column(name = "max_temperature")
    var maxTemperature: Double? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
