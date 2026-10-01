package com.peakda.server.domain.weather.repository

/** 단기예보 페이지(약 1,000행)를 한 번의 JDBC 배치로 upsert 한다. */
interface WeatherShortForecastBatchRepository {
    fun upsertAll(commands: List<WeatherShortForecastUpsertCommand>): Int
}
