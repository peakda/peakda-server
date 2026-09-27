package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.spot.repository.SpotVisibilityRow
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
     * 명소형 Spot 의 공개 여부를 연결 명소 상태에 맞춘다. 명소가 공개이고 서비스 대상 유형이면 공개, 아니면 숨긴다.
     * 명소형 Spot 의 visible 은 이 동기화만 바꾸므로(관리자 수동 숨김 없음) 명소가 다시 공개되거나 대상 유형이 넓어지면 복구된다.
     * attractionId 가 없는 명소형 Spot 은 연결 명소가 없으므로 숨긴다. 기록·찜이 달려 있을 수 있어 삭제하지 않는다.
     */
    @Transactional
    fun syncVisibility(rows: List<SpotVisibilityRow>): AttractionSpotVisibilitySyncResult {
        val attractionIds = rows.mapNotNull(SpotVisibilityRow::attractionId)
        val eligibleIds = if (attractionIds.isEmpty()) {
            emptySet()
        } else {
            attractionRepository.findVisibleIdsByIdInAndContentTypes(
                attractionIds,
                eligibilityProperties.eligibleContentTypes,
            ).toSet()
        }
        val (shouldShow, shouldHide) = rows.partition { it.attractionId in eligibleIds }
        val hideIds = shouldHide.filter { it.visible }.map(SpotVisibilityRow::id)
        val showIds = shouldShow.filterNot { it.visible }.map(SpotVisibilityRow::id)
        return AttractionSpotVisibilitySyncResult(
            hidden = if (hideIds.isEmpty()) 0 else spotRepository.updateVisibleByIdIn(hideIds, false),
            shown = if (showIds.isEmpty()) 0 else spotRepository.updateVisibleByIdIn(showIds, true),
        )
    }
}
