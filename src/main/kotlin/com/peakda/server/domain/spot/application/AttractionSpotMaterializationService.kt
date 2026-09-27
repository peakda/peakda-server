package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

/**
 * 서비스 대상 유형의 visible 명소를 페이지 단위로 읽어 좌표가 있는 명소형 Spot을 멱등적으로 materialize한다.
 * 이어서 더 이상 대상이 아닌 명소(비공개·대상 외 유형)의 명소형 Spot을 숨긴다.
 */
@Service
class AttractionSpotMaterializationService(
    private val attractionRepository: AttractionRepository,
    private val chunkService: AttractionSpotMaterializationChunkService,
    private val eligibilityProperties: AttractionEligibilityProperties,
    private val spotRepository: SpotRepository,
) {
    fun materializeVisibleAttractions(): AttractionSpotMaterializationResult {
        var pageNumber = 0
        var processed = 0
        var skippedNoCoordinates = 0
        var pages = 0
        while (true) {
            val page = attractionRepository.findByVisibleTrueAndContentTypeCodeIn(
                eligibilityProperties.eligibleContentTypes,
                PageRequest.of(pageNumber, PAGE_SIZE, Sort.by(Sort.Direction.ASC, "id")),
            )
            if (page.isEmpty) break
            val chunk = chunkService.materialize(page.content)
            processed += chunk.processed
            skippedNoCoordinates += chunk.skippedNoCoordinates
            pages++
            if (!page.hasNext()) break
            pageNumber++
        }
        val hidden = hideIneligibleAttractionSpots()
        log.info(
            "[spot-materialization] visible attractions processed={} skippedNoCoordinates={} pages={} hidden={}",
            processed,
            skippedNoCoordinates,
            pages,
            hidden,
        )
        return AttractionSpotMaterializationResult(processed, skippedNoCoordinates, pages, hidden)
    }

    private fun hideIneligibleAttractionSpots(): Int {
        var lastId = 0L
        var hidden = 0
        while (true) {
            val spots = spotRepository.findByTypeAndVisibleTrueAndIdGreaterThanOrderByIdAsc(
                SpotType.ATTRACTION,
                lastId,
                PageRequest.of(0, PAGE_SIZE),
            )
            if (spots.isEmpty()) break
            hidden += chunkService.hideIneligible(spots)
            lastId = requireNotNull(spots.last().id)
            if (spots.size < PAGE_SIZE) break
        }
        return hidden
    }

    companion object {
        private const val PAGE_SIZE = 100
        private val log = LoggerFactory.getLogger(AttractionSpotMaterializationService::class.java)
    }
}
