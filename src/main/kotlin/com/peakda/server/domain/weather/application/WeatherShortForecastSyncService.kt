package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.repository.WeatherShortForecastRepository
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.VilageFcstItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WeatherShortForecastSyncService(
    private val repository: WeatherShortForecastRepository,
) {
    @Transactional
    fun upsertPage(items: List<VilageFcstItem>): Int {
        val commands = items
            .filter { it.category.isNotBlank() && it.fcstDate.isNotBlank() && it.fcstTime.isNotBlank() }
            .map { it.toUpsertCommand() }
        return repository.upsertAll(commands)
    }

    /** [forecastDate](yyyyMMdd) 이전 예보를 지우고 지운 행 수를 반환한다. */
    @Transactional
    fun deleteBefore(forecastDate: String): Int = repository.deleteByForecastDateBefore(forecastDate)
}
