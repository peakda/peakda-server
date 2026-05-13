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
    fun upsertLand(regId: String, tmFc: String, item: MidLandFcstItem): Int {
        val entity = findOrCreate(regId, tmFc)
        entity.applyLand(item)
        return 1
    }

    @Transactional
    fun upsertTa(regId: String, tmFc: String, item: MidTaItem): Int {
        val entity = findOrCreate(regId, tmFc)
        entity.applyTa(item)
        return 1
    }

    private fun findOrCreate(regId: String, tmFc: String): WeatherMidForecast {
        return repository.findByRegIdAndTmFc(regId, tmFc)
            ?: repository.save(WeatherMidForecast(regId = regId, tmFc = tmFc))
    }
}
