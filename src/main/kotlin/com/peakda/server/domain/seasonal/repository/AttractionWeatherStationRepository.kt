package com.peakda.server.domain.seasonal.repository

import com.peakda.server.domain.seasonal.entity.AttractionWeatherStation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val ATTRACTION_WEATHER_STATION_UPSERT_SQL = """
    INSERT INTO attraction_weather_stations (
        attraction_id, station_id, distance_meters, created_at, updated_at
    ) VALUES (
        :#{#command.attractionId}, :#{#command.stationId}, :#{#command.distanceMeters}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_attraction_weather_stations_attraction DO UPDATE SET
        station_id = EXCLUDED.station_id,
        distance_meters = EXCLUDED.distance_meters,
        updated_at = now()
"""

interface AttractionWeatherStationRepository : JpaRepository<AttractionWeatherStation, Long> {
    @Modifying
    @Query(value = ATTRACTION_WEATHER_STATION_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: AttractionWeatherStationUpsertCommand): Int
}

data class AttractionWeatherStationUpsertCommand(
    val attractionId: Long,
    val stationId: String,
    val distanceMeters: Double,
)
