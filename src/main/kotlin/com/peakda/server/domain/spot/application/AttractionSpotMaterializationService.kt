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
 * 이어서 명소형 Spot 의 공개 여부를 연결 명소 상태(공개·대상 유형)에 맞춘다.
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
        val visibility = syncAttractionSpotVisibility()
        log.info(
            "[spot-materialization] visible attractions processed={} skippedNoCoordinates={} pages={} hidden={} shown={}",
            processed,
            skippedNoCoordinates,
            pages,
            visibility.hidden,
            visibility.shown,
        )
        return AttractionSpotMaterializationResult(processed, skippedNoCoordinates, pages, visibility.hidden, visibility.shown)
    }

    private fun syncAttractionSpotVisibility(): AttractionSpotVisibilitySyncResult {
        var lastId = 0L
        var hidden = 0
        var shown = 0
        while (true) {
            val rows = spotRepository.findByTypeAndIdGreaterThanOrderByIdAsc(
                SpotType.ATTRACTION,
                lastId,
                PageRequest.of(0, PAGE_SIZE),
            )
            if (rows.isEmpty()) break
            val chunk = chunkService.syncVisibility(rows)
            hidden += chunk.hidden
            shown += chunk.shown
            lastId = rows.last().id
            if (rows.size < PAGE_SIZE) break
        }
        return AttractionSpotVisibilitySyncResult(hidden, shown)
    }

    companion object {
        private const val PAGE_SIZE = 100
        private val log = LoggerFactory.getLogger(AttractionSpotMaterializationService::class.java)
    }
}
