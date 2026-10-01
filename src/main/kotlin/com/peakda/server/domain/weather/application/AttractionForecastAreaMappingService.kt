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

    /** [keptAttractionIds] 밖의 매핑을 지우고 지운 건수를 반환한다. 대상이 비면 전부 지운다. */
    @Transactional
    fun deleteExcept(keptAttractionIds: Set<Long>): Int =
        if (keptAttractionIds.isEmpty()) {
            repository.count().toInt().also { repository.deleteAllInBatch() }
        } else {
            repository.deleteByAttractionIdNotIn(keptAttractionIds)
        }

    /** 단기예보 수집 대상 격자. 명소가 많이 모인 격자부터 [limit] 개. */
    @Transactional(readOnly = true)
    fun findCollectionGrids(limit: Int): List<ForecastGrid> =
        if (limit <= 0) emptyList() else repository.findGridsByAttractionCount(PageRequest.of(0, limit))
}
