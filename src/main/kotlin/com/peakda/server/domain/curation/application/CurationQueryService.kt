package com.peakda.server.domain.curation.application

import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationChapter
import com.peakda.server.domain.curation.entity.CurationRecommendation
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.exception.CurationNotFoundException
import com.peakda.server.domain.curation.presentation.response.CurationCardResponse
import com.peakda.server.domain.curation.presentation.response.CurationDetailResponse
import com.peakda.server.domain.curation.presentation.response.CurationDetailResponse.CurationChapterResponse
import com.peakda.server.domain.curation.presentation.response.CurationDetailResponse.CurationRecommendationResponse
import com.peakda.server.domain.curation.repository.CurationChapterRepository
import com.peakda.server.domain.curation.repository.CurationRecommendationRepository
import com.peakda.server.domain.curation.repository.CurationRepository
import com.peakda.server.domain.spot.application.SpotPreviewService
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.SpotPreviewItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CurationQueryService(
    private val curationRepository: CurationRepository,
    private val curationChapterRepository: CurationChapterRepository,
    private val curationRecommendationRepository: CurationRecommendationRepository,
    private val spotPreviewService: SpotPreviewService,
) {

    /** 발행된 큐레이션 카드 목록 (최신 주차순). */
    @Transactional(readOnly = true)
    fun cards(pageable: Pageable): Page<CurationCardResponse> =
        curationRepository.findByStatusOrderByWeekStartDateDesc(CurationStatus.PUBLISHED, pageable)
            .map { it.toCardResponse() }

    /** 발행된 큐레이션 상세. 없거나 DRAFT 면 [CurationNotFoundException]. */
    @Transactional(readOnly = true)
    fun detail(id: Long, lat: Double?, lng: Double?): CurationDetailResponse {
        val curation = curationRepository.findByIdAndStatus(id, CurationStatus.PUBLISHED)
            ?: throw CurationNotFoundException()
        val chapters = curationChapterRepository.findByCurationIdOrderBySortOrderAsc(id)
        val recommendations = curationRecommendationRepository.findByCurationIdOrderBySortOrderAsc(id)
        val spotIds = (chapters.mapNotNull { it.spotId } + recommendations.mapNotNull { it.spotId }).distinct()
        val previewBySpot = spotPreviewService.preview(spotIds, category = null, lat = lat, lng = lng)
            .items
            .associateBy { it.spotId }

        return CurationDetailResponse(
            id = requireNotNull(curation.id),
            weekLabel = curation.weekLabel,
            weekStartDate = curation.weekStartDate,
            weekEndDate = curation.weekEndDate,
            title = curation.title,
            subtitle = curation.subtitle,
            heroImageUrl = curation.heroImageUrl,
            intro = curation.intro,
            nextTeaserOverline = curation.nextTeaserOverline,
            nextTeaserBody = curation.nextTeaserBody,
            chapters = chapters.map { chapter -> chapter.toResponse(chapter.spotId?.let(previewBySpot::get)) },
            recommendations = recommendations.map { recommendation ->
                recommendation.toResponse(recommendation.spotId?.let(previewBySpot::get))
            },
        )
    }

    private fun Curation.toCardResponse(): CurationCardResponse = CurationCardResponse(
        id = requireNotNull(id),
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        title = title,
        subtitle = subtitle,
        heroImageUrl = heroImageUrl,
    )

    private fun CurationChapter.toResponse(preview: SpotPreviewItem?): CurationChapterResponse =
        CurationChapterResponse(
            sortOrder = sortOrder,
            layout = layout,
            heading = heading,
            spotId = spotId,
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            photoUrl = photoUrl ?: preview?.thumbnailUrl,
            pullQuote = pullQuote,
            leadText = leadText,
            body = body,
            factNote = factNote,
            badge = preview?.badge,
            distanceMeters = preview?.distanceMeters,
        )

    private fun CurationRecommendation.toResponse(preview: SpotPreviewItem?): CurationRecommendationResponse =
        CurationRecommendationResponse(
            sortOrder = sortOrder,
            title = title,
            spotId = spotId,
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            photoUrl = photoUrl ?: preview?.thumbnailUrl,
            body = body,
            distanceMeters = preview?.distanceMeters,
        )
}
