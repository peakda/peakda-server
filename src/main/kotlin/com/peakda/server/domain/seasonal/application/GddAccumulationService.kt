package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.weather.repository.WeatherDailyObservationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GddAccumulationService(
    private val repository: WeatherDailyObservationRepository,
) {
    /** 지점별 [DailyTemperature] 목록. 배치 실행당 한 번만 호출해 재사용한다. */
    @Transactional(readOnly = true)
    fun loadDailyTemperatures(
        stationIds: Collection<String>,
        from: LocalDate,
        to: LocalDate,
    ): Map<String, List<DailyTemperature>> =
        repository.findByStationIdInAndObservedOnBetween(stationIds, from, to)
            .groupBy { observation -> observation.stationId }
            .mapValues { (_, observations) ->
                observations.map { observation ->
                    DailyTemperature(
                        observedOn = observation.observedOn,
                        avgTemperature = observation.avgTemperature,
                        minTemperature = observation.minTemperature,
                        maxTemperature = observation.maxTemperature,
                    )
                }
            }
}
