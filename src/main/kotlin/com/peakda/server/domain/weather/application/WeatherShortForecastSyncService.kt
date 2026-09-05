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
        return items
            .filter { it.category.isNotBlank() && it.fcstDate.isNotBlank() && it.fcstTime.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
