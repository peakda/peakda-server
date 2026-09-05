package com.peakda.server.domain.home.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.home.presentation.response.HomeSuggestionResponse
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 홈 검색바 보조 카피(SCR-011 #5) — 최신 산출일 기준 신뢰도가 가장 높은 절정(PEAK) 명소×카테고리 1건으로
 * "요즘 절정인 {꽃}, {명소}에서 만나보세요" 카피를 만든다. 절정 데이터가 없으면 [available] = false.
 */
@Service
class HomeSuggestionService(
    private val attractionRepository: AttractionRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
) {

    @Transactional(readOnly = true)
    fun suggestion(): HomeSuggestionResponse {
        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate()
        val best = baseDate?.let {
            seasonalBloomEstimateRepository.findByBaseDateAndStatus(it, BloomStatus.PEAK).maxByOrNull { e -> e.confidence }
        }
        val attraction = best?.let { attractionRepository.findById(it.attractionId).orElse(null) }
        if (best == null || attraction == null) return unavailable(baseDate)

        return HomeSuggestionResponse(
            available = true,
            message = "요즘 절정인 ${best.bloomCategory.displayName}, ${attraction.title}에서 만나보세요",
            category = best.bloomCategory,
            displayName = best.bloomCategory.displayName,
            attractionId = best.attractionId,
            attractionTitle = attraction.title,
            baseDate = baseDate,
        )
    }

    private fun unavailable(baseDate: LocalDate?) = HomeSuggestionResponse(
        available = false,
        message = null,
        category = null,
        displayName = null,
        attractionId = null,
        attractionTitle = null,
        baseDate = baseDate,
    )
}
