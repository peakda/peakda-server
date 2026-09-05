package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

/** visible 명소를 페이지 단위로 읽어 좌표가 있는 명소형 Spot을 멱등적으로 materialize한다. */
@Service
class AttractionSpotMaterializationService(
    private val attractionRepository: AttractionRepository,
    private val chunkService: AttractionSpotMaterializationChunkService,
) {
    fun materializeVisibleAttractions(): AttractionSpotMaterializationResult {
        var pageNumber = 0
        var processed = 0
        var skippedNoCoordinates = 0
        var pages = 0
        while (true) {
            val page = attractionRepository.findByVisibleTrue(
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
        log.info(
            "[spot-materialization] visible attractions processed={} skippedNoCoordinates={} pages={}",
            processed,
            skippedNoCoordinates,
            pages,
        )
        return AttractionSpotMaterializationResult(processed, skippedNoCoordinates, pages)
    }

    companion object {
        private const val PAGE_SIZE = 100
        private val log = LoggerFactory.getLogger(AttractionSpotMaterializationService::class.java)
    }
}
