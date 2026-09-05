package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.repository.WeatherMidForecastRepository
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidLandFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidTaItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WeatherMidForecastSyncService(
    private val repository: WeatherMidForecastRepository,
) {
    @Transactional
    fun upsertLandForecast(
        regionCode: String,
        sourceRegionCode: String,
        announceTime: String,
        item: MidLandFcstItem,
    ): Int {
        return repository.upsertLand(item.toUpsertCommand(regionCode, sourceRegionCode, announceTime))
    }

    @Transactional
    fun upsertTemperatureForecast(
        regionCode: String,
        sourceRegionCode: String,
        announceTime: String,
        item: MidTaItem,
    ): Int {
        return repository.upsertTemperature(item.toUpsertCommand(regionCode, sourceRegionCode, announceTime))
    }
}
