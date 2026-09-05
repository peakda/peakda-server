package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.presentation.response.BloomPeakListResponse
import com.peakda.server.domain.seasonal.presentation.response.BloomPeakListResponse.BloomPeakItem
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Q1 산출물 조회 — 절정 명소 리스트. 최신 산출일(base_date) 기준의 읽기 전용 조회.
 *
 * 지도(영역) Spot 핀 조회는 [SpotBloomMapService] 가 담당한다.
 */
@Service
class BloomQueryService(
    private val attractionRepository: AttractionRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
) {
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
}
