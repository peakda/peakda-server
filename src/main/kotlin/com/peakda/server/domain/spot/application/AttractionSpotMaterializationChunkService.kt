package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 한 페이지의 Spot materialization을 한 트랜잭션으로 커밋한다. */
@Service
class AttractionSpotMaterializationChunkService(
    private val spotService: SpotService,
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
}
