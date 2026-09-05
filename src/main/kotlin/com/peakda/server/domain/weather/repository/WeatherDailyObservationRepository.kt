package com.peakda.server.domain.weather.repository

import com.peakda.server.domain.weather.entity.WeatherDailyObservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

private const val WEATHER_DAILY_OBSERVATION_UPSERT_SQL = """
    INSERT INTO weather_daily_observations (
        station_id, observed_on, station_name, avg_temperature, min_temperature, max_temperature,
        created_at, updated_at
    ) VALUES (
        :#{#command.stationId}, :#{#command.observedOn}, :#{#command.stationName},
        :#{#command.avgTemperature}, :#{#command.minTemperature}, :#{#command.maxTemperature},
        now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_weather_daily_observations_station_date DO UPDATE SET
        station_name = EXCLUDED.station_name,
        avg_temperature = EXCLUDED.avg_temperature,
        min_temperature = EXCLUDED.min_temperature,
        max_temperature = EXCLUDED.max_temperature,
        updated_at = now()
"""

interface WeatherDailyObservationRepository : JpaRepository<WeatherDailyObservation, Long> {
    fun findByStationIdInAndObservedOnBetween(
        stationIds: Collection<String>,
        from: LocalDate,
        to: LocalDate,
    ): List<WeatherDailyObservation>

    @Modifying
    @Query(value = WEATHER_DAILY_OBSERVATION_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: WeatherDailyObservationUpsertCommand): Int

    @Query(
        """
        SELECT o.stationId AS stationId, MAX(o.observedOn) AS latestObservedOn
        FROM WeatherDailyObservation o
        GROUP BY o.stationId
        """,
    )
    fun findLatestObservedOnByStation(): List<StationLatestObservation>
}

interface StationLatestObservation {
    val stationId: String
    val latestObservedOn: LocalDate
}

data class WeatherDailyObservationUpsertCommand(
    val stationId: String,
    val observedOn: LocalDate,
    val stationName: String?,
    val avgTemperature: Double?,
    val minTemperature: Double?,
    val maxTemperature: Double?,
)
