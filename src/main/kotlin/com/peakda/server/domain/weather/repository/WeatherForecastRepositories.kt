package com.peakda.server.domain.weather.repository

import com.peakda.server.domain.weather.entity.WeatherMidForecast
import com.peakda.server.domain.weather.entity.WeatherShortForecast
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val WEATHER_SHORT_FORECAST_UPSERT_SQL = """
    INSERT INTO weather_short_forecasts (
        grid_x, grid_y, announce_date, announce_time, forecast_date, forecast_time,
        forecast_category, forecast_value, created_at, updated_at
    ) VALUES (
        :#{#command.gridX}, :#{#command.gridY}, :#{#command.announceDate}, :#{#command.announceTime},
        :#{#command.forecastDate}, :#{#command.forecastTime}, :#{#command.forecastCategory},
        :#{#command.forecastValue}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_weather_short_forecasts_grid_forecast DO UPDATE SET
        announce_date = EXCLUDED.announce_date,
        announce_time = EXCLUDED.announce_time,
        forecast_value = EXCLUDED.forecast_value,
        updated_at = now()
"""

private const val WEATHER_MID_LAND_UPSERT_SQL = """
    INSERT INTO weather_mid_forecasts (
        region_code, announce_time, source_land_region_code,
        weather_day3_am, weather_day3_pm, weather_day4_am, weather_day4_pm, weather_day5_am, weather_day5_pm,
        weather_day6_am, weather_day6_pm, weather_day7_am, weather_day7_pm, weather_day8, weather_day9, weather_day10,
        rain_probability_day3_am, rain_probability_day3_pm, rain_probability_day4_am, rain_probability_day4_pm,
        rain_probability_day5_am, rain_probability_day5_pm, rain_probability_day6_am, rain_probability_day6_pm,
        rain_probability_day7_am, rain_probability_day7_pm, rain_probability_day8, rain_probability_day9, rain_probability_day10,
        created_at, updated_at
    ) VALUES (
        :#{#command.regionCode}, :#{#command.announceTime}, :#{#command.sourceLandRegionCode},
        :#{#command.weatherDay3Am}, :#{#command.weatherDay3Pm}, :#{#command.weatherDay4Am}, :#{#command.weatherDay4Pm},
        :#{#command.weatherDay5Am}, :#{#command.weatherDay5Pm}, :#{#command.weatherDay6Am}, :#{#command.weatherDay6Pm},
        :#{#command.weatherDay7Am}, :#{#command.weatherDay7Pm}, :#{#command.weatherDay8}, :#{#command.weatherDay9},
        :#{#command.weatherDay10}, :#{#command.rainProbabilityDay3Am}, :#{#command.rainProbabilityDay3Pm},
        :#{#command.rainProbabilityDay4Am}, :#{#command.rainProbabilityDay4Pm}, :#{#command.rainProbabilityDay5Am},
        :#{#command.rainProbabilityDay5Pm}, :#{#command.rainProbabilityDay6Am}, :#{#command.rainProbabilityDay6Pm},
        :#{#command.rainProbabilityDay7Am}, :#{#command.rainProbabilityDay7Pm}, :#{#command.rainProbabilityDay8},
        :#{#command.rainProbabilityDay9}, :#{#command.rainProbabilityDay10}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_weather_mid_forecasts_region_announce DO UPDATE SET
        source_land_region_code = EXCLUDED.source_land_region_code,
        weather_day3_am = COALESCE(EXCLUDED.weather_day3_am, weather_mid_forecasts.weather_day3_am),
        weather_day3_pm = COALESCE(EXCLUDED.weather_day3_pm, weather_mid_forecasts.weather_day3_pm),
        weather_day4_am = COALESCE(EXCLUDED.weather_day4_am, weather_mid_forecasts.weather_day4_am),
        weather_day4_pm = COALESCE(EXCLUDED.weather_day4_pm, weather_mid_forecasts.weather_day4_pm),
        weather_day5_am = COALESCE(EXCLUDED.weather_day5_am, weather_mid_forecasts.weather_day5_am),
        weather_day5_pm = COALESCE(EXCLUDED.weather_day5_pm, weather_mid_forecasts.weather_day5_pm),
        weather_day6_am = COALESCE(EXCLUDED.weather_day6_am, weather_mid_forecasts.weather_day6_am),
        weather_day6_pm = COALESCE(EXCLUDED.weather_day6_pm, weather_mid_forecasts.weather_day6_pm),
        weather_day7_am = COALESCE(EXCLUDED.weather_day7_am, weather_mid_forecasts.weather_day7_am),
        weather_day7_pm = COALESCE(EXCLUDED.weather_day7_pm, weather_mid_forecasts.weather_day7_pm),
        weather_day8 = COALESCE(EXCLUDED.weather_day8, weather_mid_forecasts.weather_day8),
        weather_day9 = COALESCE(EXCLUDED.weather_day9, weather_mid_forecasts.weather_day9),
        weather_day10 = COALESCE(EXCLUDED.weather_day10, weather_mid_forecasts.weather_day10),
        rain_probability_day3_am = EXCLUDED.rain_probability_day3_am,
        rain_probability_day3_pm = EXCLUDED.rain_probability_day3_pm,
        rain_probability_day4_am = EXCLUDED.rain_probability_day4_am,
        rain_probability_day4_pm = EXCLUDED.rain_probability_day4_pm,
        rain_probability_day5_am = EXCLUDED.rain_probability_day5_am,
        rain_probability_day5_pm = EXCLUDED.rain_probability_day5_pm,
        rain_probability_day6_am = EXCLUDED.rain_probability_day6_am,
        rain_probability_day6_pm = EXCLUDED.rain_probability_day6_pm,
        rain_probability_day7_am = EXCLUDED.rain_probability_day7_am,
        rain_probability_day7_pm = EXCLUDED.rain_probability_day7_pm,
        rain_probability_day8 = EXCLUDED.rain_probability_day8,
        rain_probability_day9 = EXCLUDED.rain_probability_day9,
        rain_probability_day10 = EXCLUDED.rain_probability_day10,
        updated_at = now()
"""

private const val WEATHER_MID_TEMPERATURE_UPSERT_SQL = """
    INSERT INTO weather_mid_forecasts (
        region_code, announce_time, source_temperature_region_code,
        temperature_min_day3, temperature_max_day3, temperature_min_day4, temperature_max_day4,
        temperature_min_day5, temperature_max_day5, temperature_min_day6, temperature_max_day6,
        temperature_min_day7, temperature_max_day7, temperature_min_day8, temperature_max_day8,
        temperature_min_day9, temperature_max_day9, temperature_min_day10, temperature_max_day10,
        created_at, updated_at
    ) VALUES (
        :#{#command.regionCode}, :#{#command.announceTime}, :#{#command.sourceTemperatureRegionCode},
        :#{#command.temperatureMinDay3}, :#{#command.temperatureMaxDay3}, :#{#command.temperatureMinDay4},
        :#{#command.temperatureMaxDay4}, :#{#command.temperatureMinDay5}, :#{#command.temperatureMaxDay5},
        :#{#command.temperatureMinDay6}, :#{#command.temperatureMaxDay6}, :#{#command.temperatureMinDay7},
        :#{#command.temperatureMaxDay7}, :#{#command.temperatureMinDay8}, :#{#command.temperatureMaxDay8},
        :#{#command.temperatureMinDay9}, :#{#command.temperatureMaxDay9}, :#{#command.temperatureMinDay10},
        :#{#command.temperatureMaxDay10}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_weather_mid_forecasts_region_announce DO UPDATE SET
        source_temperature_region_code = EXCLUDED.source_temperature_region_code,
        temperature_min_day3 = EXCLUDED.temperature_min_day3,
        temperature_max_day3 = EXCLUDED.temperature_max_day3,
        temperature_min_day4 = EXCLUDED.temperature_min_day4,
        temperature_max_day4 = EXCLUDED.temperature_max_day4,
        temperature_min_day5 = EXCLUDED.temperature_min_day5,
        temperature_max_day5 = EXCLUDED.temperature_max_day5,
        temperature_min_day6 = EXCLUDED.temperature_min_day6,
        temperature_max_day6 = EXCLUDED.temperature_max_day6,
        temperature_min_day7 = EXCLUDED.temperature_min_day7,
        temperature_max_day7 = EXCLUDED.temperature_max_day7,
        temperature_min_day8 = EXCLUDED.temperature_min_day8,
        temperature_max_day8 = EXCLUDED.temperature_max_day8,
        temperature_min_day9 = EXCLUDED.temperature_min_day9,
        temperature_max_day9 = EXCLUDED.temperature_max_day9,
        temperature_min_day10 = EXCLUDED.temperature_min_day10,
        temperature_max_day10 = EXCLUDED.temperature_max_day10,
        updated_at = now()
"""

interface WeatherMidForecastRepository : JpaRepository<WeatherMidForecast, Long> {
    fun findByRegionCodeAndAnnounceTime(regionCode: String, announceTime: String): WeatherMidForecast?

    fun findFirstByRegionCodeOrderByAnnounceTimeDesc(regionCode: String): WeatherMidForecast?

    @Modifying
    @Query(value = WEATHER_MID_LAND_UPSERT_SQL, nativeQuery = true)
    fun upsertLand(@Param("command") command: WeatherMidLandForecastUpsertCommand): Int

    @Modifying
    @Query(value = WEATHER_MID_TEMPERATURE_UPSERT_SQL, nativeQuery = true)
    fun upsertTemperature(@Param("command") command: WeatherMidTemperatureForecastUpsertCommand): Int
}

interface WeatherShortForecastRepository : JpaRepository<WeatherShortForecast, Long> {
    fun findByGridXAndGridYAndForecastDateAndForecastTimeAndForecastCategory(
        gridX: Int,
        gridY: Int,
        forecastDate: String,
        forecastTime: String,
        forecastCategory: String,
    ): WeatherShortForecast?

    fun findByGridXAndGridYAndForecastCategoryInAndForecastDateBetween(
        gridX: Int,
        gridY: Int,
        forecastCategories: Collection<String>,
        forecastDateStart: String,
        forecastDateEnd: String,
    ): List<WeatherShortForecast>

    @Modifying
    @Query(value = WEATHER_SHORT_FORECAST_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: WeatherShortForecastUpsertCommand): Int
}

data class WeatherShortForecastUpsertCommand(
    val gridX: Int,
    val gridY: Int,
    val announceDate: String,
    val announceTime: String,
    val forecastDate: String,
    val forecastTime: String,
    val forecastCategory: String,
    val forecastValue: String,
)

data class WeatherMidLandForecastUpsertCommand(
    val regionCode: String,
    val sourceLandRegionCode: String,
    val announceTime: String,
    val weatherDay3Am: String?,
    val weatherDay3Pm: String?,
    val weatherDay4Am: String?,
    val weatherDay4Pm: String?,
    val weatherDay5Am: String?,
    val weatherDay5Pm: String?,
    val weatherDay6Am: String?,
    val weatherDay6Pm: String?,
    val weatherDay7Am: String?,
    val weatherDay7Pm: String?,
    val weatherDay8: String?,
    val weatherDay9: String?,
    val weatherDay10: String?,
    val rainProbabilityDay3Am: Int,
    val rainProbabilityDay3Pm: Int,
    val rainProbabilityDay4Am: Int,
    val rainProbabilityDay4Pm: Int,
    val rainProbabilityDay5Am: Int,
    val rainProbabilityDay5Pm: Int,
    val rainProbabilityDay6Am: Int,
    val rainProbabilityDay6Pm: Int,
    val rainProbabilityDay7Am: Int,
    val rainProbabilityDay7Pm: Int,
    val rainProbabilityDay8: Int,
    val rainProbabilityDay9: Int,
    val rainProbabilityDay10: Int,
)

data class WeatherMidTemperatureForecastUpsertCommand(
    val regionCode: String,
    val sourceTemperatureRegionCode: String,
    val announceTime: String,
    val temperatureMinDay3: Int,
    val temperatureMaxDay3: Int,
    val temperatureMinDay4: Int,
    val temperatureMaxDay4: Int,
    val temperatureMinDay5: Int,
    val temperatureMaxDay5: Int,
    val temperatureMinDay6: Int,
    val temperatureMaxDay6: Int,
    val temperatureMinDay7: Int,
    val temperatureMaxDay7: Int,
    val temperatureMinDay8: Int,
    val temperatureMaxDay8: Int,
    val temperatureMinDay9: Int,
    val temperatureMaxDay9: Int,
    val temperatureMinDay10: Int,
    val temperatureMaxDay10: Int,
)
