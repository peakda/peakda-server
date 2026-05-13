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
        var saved = 0
        for (item in items) {
            if (item.category.isBlank() || item.fcstDate.isBlank() || item.fcstTime.isBlank()) continue
            val existing = repository.findByNxAndNyAndFcstDateAndFcstTimeAndCategory(
                item.nx,
                item.ny,
                item.fcstDate,
                item.fcstTime,
                item.category,
            )
            if (existing == null) {
                repository.save(item.toShortForecast())
            } else {
                existing.applyUpdate(item)
            }
            saved++
        }
        return saved
    }
}
