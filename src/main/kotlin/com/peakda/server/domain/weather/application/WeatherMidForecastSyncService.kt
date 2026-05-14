package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.entity.WeatherMidForecast
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
    fun upsertLand(regionCode: String, sourceRegionCode: String, announceTime: String, item: MidLandFcstItem): Int {
        val entity = findOrCreate(regionCode, announceTime)
        entity.sourceLandRegionCode = sourceRegionCode
        entity.applyLand(item)
        return 1
    }

    @Transactional
    fun upsertTa(regionCode: String, sourceRegionCode: String, announceTime: String, item: MidTaItem): Int {
        val entity = findOrCreate(regionCode, announceTime)
        entity.sourceTemperatureRegionCode = sourceRegionCode
        entity.applyTa(item)
        return 1
    }

    private fun findOrCreate(regionCode: String, announceTime: String): WeatherMidForecast {
        return repository.findByRegionCodeAndAnnounceTime(regionCode, announceTime)
            ?: repository.save(WeatherMidForecast(regionCode = regionCode, announceTime = announceTime))
    }
}
