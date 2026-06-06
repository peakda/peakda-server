package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse.BloomMapItem
import com.peakda.server.domain.seasonal.presentation.response.BloomMapResponse.BloomSlot
import com.peakda.server.domain.seasonal.presentation.response.BloomPeakListResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomPeakListResponse.BloomPeakItem
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Q1 산출물 조회 — 지도(영역) 현재 상태, 절정 명소 리스트. 모두 최신 산출일(base_date) 기준의 읽기 전용 조회.
 */
@Service
class BloomQueryService(
    private val attractionRepository: AttractionRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
) {
    /** 지도 영역(bbox) 내 visible 명소의 현재 개화 슬롯. ENDED 는 핀에서 제외한다. */
    @Transactional(readOnly = true)
    fun mapByBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        category: BloomCategory?,
    ): BloomMapResponse {
        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate()
            ?: return BloomMapResponse(baseDate = null, count = 0, attractions = emptyList())

        val attractionsById = attractionRepository
            .findVisibleInBoundingBox(minLat = minLat, maxLat = maxLat, minLng = minLng, maxLng = maxLng)
            .associateBy { it.id }
        if (attractionsById.isEmpty()) return BloomMapResponse(baseDate, 0, emptyList())

        val ids = attractionsById.keys.filterNotNull()
        val estimates = if (category != null) {
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdInAndBloomCategory(baseDate, ids, category)
        } else {
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, ids)
        }

        val items = estimates
            .filter { it.status != BloomStatus.ENDED }
            .groupBy { it.attractionId }
            .mapNotNull { (attractionId, rows) ->
                val attraction = attractionsById[attractionId] ?: return@mapNotNull null
                BloomMapItem(
                    attractionId = attractionId,
                    title = attraction.title,
                    latitude = attraction.latitude,
                    longitude = attraction.longitude,
                    blooms = rows.map { it.toSlot() },
                )
            }
        return BloomMapResponse(baseDate = baseDate, count = items.size, attractions = items)
    }

    /** 최신 산출일 기준 status=PEAK 명소 리스트. "지금이 절정이에요" 공급. */
    @Transactional(readOnly = true)
    fun peakList(category: BloomCategory?): BloomPeakListResponse {
        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate()
            ?: return BloomPeakListResponse(baseDate = null, count = 0, items = emptyList())

        val estimates = if (category != null) {
            seasonalBloomEstimateRepository.findByBaseDateAndStatusAndBloomCategory(baseDate, BloomStatus.PEAK, category)
        } else {
            seasonalBloomEstimateRepository.findByBaseDateAndStatus(baseDate, BloomStatus.PEAK)
        }
        if (estimates.isEmpty()) return BloomPeakListResponse(baseDate, 0, emptyList())

        val attractionsById = attractionRepository
            .findAllById(estimates.map { it.attractionId })
            .associateBy { it.id }
        val items = estimates.mapNotNull { estimate ->
            val attraction = attractionsById[estimate.attractionId] ?: return@mapNotNull null
            BloomPeakItem(
                attractionId = estimate.attractionId,
                title = attraction.title,
                latitude = attraction.latitude,
                longitude = attraction.longitude,
                category = estimate.bloomCategory,
                displayName = estimate.bloomCategory.displayName,
                confidence = estimate.confidence,
                peakStartDate = estimate.peakStartDate,
                peakEndDate = estimate.peakEndDate,
                peakDurationDays = estimate.peakDurationDays,
            )
        }
        return BloomPeakListResponse(baseDate = baseDate, count = items.size, items = items)
    }

    private fun SeasonalBloomEstimate.toSlot() = BloomSlot(
        category = bloomCategory,
        displayName = bloomCategory.displayName,
        status = status,
        confidence = confidence,
    )
}
