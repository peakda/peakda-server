package com.peakda.server.domain.weather.application

import com.peakda.server.domain.weather.repository.WeatherDailyObservationRepository
import com.peakda.server.infrastructure.external.kma.asosdaly.response.AsosDalyItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class WeatherDailyObservationSyncService(
    private val repository: WeatherDailyObservationRepository,
) {
    @Transactional
    fun upsertPage(items: List<AsosDalyItem>): Int {
        return items.mapNotNull { it.toUpsertCommand() }
            .sumOf { repository.upsert(it) }
    }

    fun findLatestObservedOnByStation(): Map<String, LocalDate> {
        return repository.findLatestObservedOnByStation()
            .associate { it.stationId to it.latestObservedOn }
    }
}
