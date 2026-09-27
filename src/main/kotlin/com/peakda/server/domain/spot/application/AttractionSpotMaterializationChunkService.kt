package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 한 페이지의 Spot materialization을 한 트랜잭션으로 커밋한다. */
@Service
class AttractionSpotMaterializationChunkService(
    private val spotService: SpotService,
    private val spotRepository: SpotRepository,
    private val attractionRepository: AttractionRepository,
    private val eligibilityProperties: AttractionEligibilityProperties,
) {
    @Transactional
    fun materialize(attractions: List<Attraction>): AttractionSpotMaterializationChunkResult {
        var processed = 0
        var skippedNoCoordinates = 0
        attractions.forEach { attraction ->
            if (attraction.latitude == null || attraction.longitude == null) {
                skippedNoCoordinates++
                return@forEach
            }
            spotService.findOrCreateForAttraction(attraction)
            processed++
        }
        return AttractionSpotMaterializationChunkResult(processed, skippedNoCoordinates)
    }

    /**
     * 명소형 Spot 중 연결된 명소가 비공개이거나 서비스 대상 유형이 아니면 숨기고 숨긴 수를 반환한다.
     * 기록·찜이 달려 있을 수 있어 삭제하지 않는다.
     */
    @Transactional
    fun hideIneligible(spots: List<Spot>): Int {
        val attractionIds = spots.mapNotNull(Spot::attractionId)
        val eligibleIds = if (attractionIds.isEmpty()) {
            emptySet()
        } else {
            attractionRepository.findVisibleIdsByIdInAndContentTypes(
                attractionIds,
                eligibilityProperties.eligibleContentTypes,
            ).toSet()
        }
        val hideIds = spots.filter { it.attractionId !in eligibleIds }.mapNotNull(Spot::id)
        return if (hideIds.isEmpty()) 0 else spotRepository.hideByIdIn(hideIds)
    }
}
