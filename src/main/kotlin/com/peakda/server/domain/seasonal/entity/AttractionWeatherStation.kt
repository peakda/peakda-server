package com.peakda.server.domain.seasonal.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/** 명소별 GDD에 사용할 최근접 ASOS 관측지점 매핑. */
@Entity
@Table(
    name = "attraction_weather_stations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_attraction_weather_stations_attraction",
            columnNames = ["attraction_id"],
        ),
    ],
)
class AttractionWeatherStation(
    @Column(name = "attraction_id", nullable = false)
    val attractionId: Long,

    @Column(name = "station_id", nullable = false, columnDefinition = "TEXT")
    var stationId: String,

    @Column(name = "distance_meters", nullable = false)
    var distanceMeters: Double,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
