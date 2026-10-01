package com.peakda.server.domain.weather.repository

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * [WeatherShortForecastRepository] 의 배치 upsert 조각.
 *
 * 네이티브 `@Modifying` 쿼리는 Hibernate JDBC 배치가 적용되지 않아 행마다 왕복한다. 단기예보는 격자당
 * 한 번에 약 1,000행이고 하루 8회 수집하므로, 격자가 늘면 왕복 수가 그대로 늘어난다.
 */
class WeatherShortForecastBatchRepositoryImpl(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : WeatherShortForecastBatchRepository {
    override fun upsertAll(commands: List<WeatherShortForecastUpsertCommand>): Int {
        if (commands.isEmpty()) return 0
        val params = commands.map(::BeanPropertySqlParameterSource).toTypedArray()
        jdbcTemplate.batchUpdate(BATCH_UPSERT_SQL, params)
        return commands.size
    }

    companion object {
        private const val BATCH_UPSERT_SQL = """
            INSERT INTO weather_short_forecasts (
                grid_x, grid_y, announce_date, announce_time, forecast_date, forecast_time,
                forecast_category, forecast_value, created_at, updated_at
            ) VALUES (
                :gridX, :gridY, :announceDate, :announceTime, :forecastDate, :forecastTime,
                :forecastCategory, :forecastValue, now(), now()
            )
            ON CONFLICT ON CONSTRAINT uk_weather_short_forecasts_grid_forecast DO UPDATE SET
                announce_date = EXCLUDED.announce_date,
                announce_time = EXCLUDED.announce_time,
                forecast_value = EXCLUDED.forecast_value,
                updated_at = now()
        """
    }
}
