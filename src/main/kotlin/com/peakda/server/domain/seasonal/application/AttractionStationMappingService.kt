package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.repository.AttractionWeatherStationRepository
import com.peakda.server.domain.seasonal.repository.AttractionWeatherStationUpsertCommand
import com.peakda.server.infrastructure.external.kma.asosdaly.AsosStationCatalog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AttractionStationMappingService(
    private val repository: AttractionWeatherStationRepository,
    private val catalog: AsosStationCatalog,
) {
    /** 좌표를 가진 명소를 최근접 지점에 매핑해 upsert 하고 처리한 건수를 반환. */
    @Transactional
    fun mapPage(attractions: List<Attraction>): Int {
        var count = 0
        for (attraction in attractions) {
            val attractionId = attraction.id ?: continue
            val latitude = attraction.latitude ?: continue
            val longitude = attraction.longitude ?: continue
            val nearest = NearestStationResolver.resolve(latitude, longitude, catalog.all) ?: continue
            repository.upsert(
                AttractionWeatherStationUpsertCommand(
                    attractionId = attractionId,
                    stationId = nearest.stationId,
                    distanceMeters = nearest.distanceMeters,
                ),
            )
            count++
        }
        return count
    }

    /** 명소 → 지점 매핑 전체. 배치 실행당 한 번만 호출한다. */
    @Transactional(readOnly = true)
    fun findStationByAttraction(): Map<Long, String> = repository.findAll()
        .associate { mapping -> mapping.attractionId to mapping.stationId }

    /**
     * 명소 하나의 관측지점. 요청당 호출되는 경로에서 쓴다 —
     * 전체 매핑을 읽는 [findStationByAttraction]은 배치 전용이다.
     */
    @Transactional(readOnly = true)
    fun findStationId(attractionId: Long): String? = repository.findByAttractionId(attractionId)?.stationId
}
