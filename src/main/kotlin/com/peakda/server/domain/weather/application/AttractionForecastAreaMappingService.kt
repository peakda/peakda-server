package com.peakda.server.domain.weather.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.weather.repository.AttractionForecastAreaRepository
import com.peakda.server.domain.weather.repository.AttractionForecastAreaUpsertCommand
import com.peakda.server.domain.weather.repository.ForecastGrid
import com.peakda.server.infrastructure.external.kma.GridConverter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AttractionForecastAreaMappingService(
    private val repository: AttractionForecastAreaRepository,
    private val gridConverter: GridConverter,
) {
    /** 좌표가 있는 명소를 단기예보 격자·중기예보 구역에 매핑해 upsert 하고 처리한 건수를 반환한다. */
    @Transactional
    fun mapPage(attractions: List<Attraction>): Int {
        var count = 0
        for (attraction in attractions) {
            val attractionId = attraction.id ?: continue
            val latitude = attraction.latitude ?: continue
            val longitude = attraction.longitude ?: continue
            val grid = gridConverter.toGrid(latitude, longitude)
            repository.upsert(
                AttractionForecastAreaUpsertCommand(
                    attractionId = attractionId,
                    gridX = grid.nx,
                    gridY = grid.ny,
                    midRegionCode = MidForecastRegionResolver.resolve(
                        attraction.legalDongAreaCode,
                        attraction.legalDongSigunguCode,
                    ),
                ),
            )
            count++
        }
        return count
    }

    /**
     * [keptAttractionIds] 밖의 매핑을 지우고 지운 건수를 반환한다.
     * 대상이 비면 지우지 않는다 — 꽃 태깅이 재적재 중이라 잠깐 비었을 때 전체 매핑이 날아가는 것을 막는다.
     */
    @Transactional
    fun deleteExcept(keptAttractionIds: Set<Long>): Int =
        if (keptAttractionIds.isEmpty()) 0 else repository.deleteByAttractionIdNotIn(keptAttractionIds)

    /** 단기예보 수집 대상 격자. 명소가 많이 모인 격자부터 [limit] 개. */
    @Transactional(readOnly = true)
    fun findCollectionGrids(limit: Int): List<ForecastGrid> =
        if (limit <= 0) emptyList() else repository.findGridsByAttractionCount(PageRequest.of(0, limit))
}
