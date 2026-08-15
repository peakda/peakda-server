package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.peakDurationDaysInclusive
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.SpotNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse.BloomBanner
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse.FavoriteState
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 스팟 상세 화면(SCR-025) 조회 — 단일 스팟의 대표 사진, 올해 만개 시기 배너(개화 추정 연동),
 * 게시된 방문 기록 수와 최신 프리뷰, 현재 사용자의 찜 상태를 한 번에 조합한다.
 */
@Service
class SpotDetailService(
    private val spotRepository: SpotRepository,
    private val attractionRepository: AttractionRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val spotRecordResponseAssembler: SpotRecordResponseAssembler,
) {

    @Transactional(readOnly = true)
    fun getDetail(spotId: Long, userId: Long): SpotDetailResponse {
        val spot = spotRepository.findById(spotId).orElseThrow { SpotNotFoundException() }

        val recordCount = spotRecordRepository.countBySpotIdAndStatus(spotId, SpotRecordStatus.PUBLISHED)
        val previewRecords = spotRecordRepository
            .findBySpotIdAndStatusOrderByCreatedAtDesc(spotId, SpotRecordStatus.PUBLISHED, PageRequest.of(0, PREVIEW_SIZE))
            .content
        val recordPreview = spotRecordResponseAssembler.assembleSummaries(previewRecords, userId)

        return SpotDetailResponse(
            id = requireNotNull(spot.id),
            type = spot.type,
            name = spot.name,
            address = spot.address,
            latitude = spot.latitude,
            longitude = spot.longitude,
            attractionId = spot.attractionId,
            representativeImageUrl = resolveRepresentativeImage(spot, recordPreview),
            bloom = resolveBloomBanner(spot),
            recordCount = recordCount,
            recordPreview = recordPreview,
            favorite = resolveFavorite(spotId, userId),
        )
    }

    /** ATTRACTION 은 명소 이미지를 우선 쓰고, 없거나 LOCAL 이면 최근 방문 기록의 대표 사진으로 대체한다. */
    private fun resolveRepresentativeImage(spot: Spot, preview: List<SpotRecordSummaryResponse>): String? {
        if (spot.type == SpotType.ATTRACTION) {
            val attractionId = spot.attractionId
            if (attractionId != null) {
                val attraction = attractionRepository.findById(attractionId).orElse(null)
                attraction?.let { it.primaryImageUrl ?: it.thumbnailImageUrl }?.let { return it }
            }
        }
        return preview.firstNotNullOfOrNull { it.coverPhoto?.url }
    }

    /** 명소에 연결된 스팟만 개화 추정을 가진다. 최신 산출일 기준 가장 강한(상태 우선·신뢰도) 추정 1건을 채택한다. */
    private fun resolveBloomBanner(spot: Spot): BloomBanner? {
        val attractionId = spot.attractionId ?: return null
        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate() ?: return null
        val representative = seasonalBloomEstimateRepository
            .findByAttractionIdAndBaseDate(attractionId, baseDate)
            .minWithOrNull(compareBy({ statusRank(it.status) }, { -it.confidence }))
            ?: return null
        return representative.toBanner(baseDate)
    }

    private fun resolveFavorite(spotId: Long, userId: Long): FavoriteState {
        val favorite = spotFavoriteRepository.findByUserIdAndSpotId(userId, spotId)
        return FavoriteState(
            favorited = favorite != null,
            notifyEnabled = favorite?.notifyEnabled ?: false,
        )
    }

    private fun SeasonalBloomEstimate.toBanner(baseDate: LocalDate) = BloomBanner(
        category = bloomCategory,
        displayName = bloomCategory.displayName,
        status = status,
        confidence = confidence,
        peakStartDate = peakStartDate,
        peakEndDate = peakEndDate,
        peakDurationDays = peakDurationDaysInclusive(peakStartDate, peakEndDate),
        baseDate = baseDate,
    )

    companion object {
        private const val PREVIEW_SIZE = 3

        private fun statusRank(status: BloomStatus): Int = when (status) {
            BloomStatus.PEAK -> 0
            BloomStatus.STARTED -> 1
            BloomStatus.PREPARING -> 2
            BloomStatus.ENDED -> 3
        }
    }
}
