package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.repository.AttractionOperatingInfoRepository
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.application.BloomBaseDateResolver
import com.peakda.server.domain.seasonal.application.BloomEstimateOrdering
import com.peakda.server.domain.seasonal.application.peakDurationDaysInclusive
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.SpotNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse.BloomBanner
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse.FavoriteState
import com.peakda.server.domain.spot.presentation.response.SpotDetailResponse.OperatingInfo
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.visittiming.application.VisitTimingBloom
import com.peakda.server.domain.visittiming.application.VisitTimingService
import com.peakda.server.domain.visittiming.presentation.response.VisitTimingResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 스팟 상세 화면(SCR-025) 조회 — 단일 스팟의 대표 사진, 올해 만개 시기 배너(개화 추정 연동),
 * 방문 타이밍(혼잡도·날씨·주변 축제), 운영 정보, 게시된 방문 기록 수와 최신 프리뷰, 현재 사용자의 찜 상태를 한 번에 조합한다.
 */
@Service
class SpotDetailService(
    private val spotRepository: SpotRepository,
    private val attractionRepository: AttractionRepository,
    private val attractionOperatingInfoRepository: AttractionOperatingInfoRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val bloomBaseDateResolver: BloomBaseDateResolver,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val spotRecordResponseAssembler: SpotRecordResponseAssembler,
    private val visitTimingService: VisitTimingService,
) {

    @Transactional(readOnly = true)
    fun getDetail(spotId: Long, userId: Long?): SpotDetailResponse {
        val spot = spotRepository.findById(spotId).orElseThrow { SpotNotFoundException() }

        val recordCount = spotRecordRepository.countBySpotIdAndStatus(spotId, SpotRecordStatus.PUBLISHED)
        val previewRecords = spotRecordRepository
            .findBySpotIdAndStatusOrderByCreatedAtDesc(spotId, SpotRecordStatus.PUBLISHED, PageRequest.of(0, PREVIEW_SIZE))
            .content
        val recordPreview = spotRecordResponseAssembler.assembleSummaries(previewRecords, userId)
        val bloom = resolveBloomBanner(spot)

        return SpotDetailResponse(
            id = requireNotNull(spot.id),
            type = spot.type,
            name = spot.name,
            address = spot.address,
            latitude = spot.latitude,
            longitude = spot.longitude,
            attractionId = spot.attractionId,
            representativeImageUrl = resolveRepresentativeImage(spot, recordPreview),
            bloom = bloom,
            visitTiming = resolveVisitTiming(spot, bloom),
            operatingInfo = resolveOperatingInfo(spot),
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

    /**
     * 명소에 연결된 스팟만 개화 추정을 가진다. 최신 산출일 기준 가장 신뢰도 높은 추정 1건을 채택한다.
     *
     * 다섯 단계를 모두 배너로 쓰며, 대표 선택 기준은 프리뷰·검색·찜 카드와 같다.
     */
    private fun resolveBloomBanner(spot: Spot): BloomBanner? {
        val attractionId = spot.attractionId ?: return null
        val baseDate = bloomBaseDateResolver.currentBaseDate() ?: return null
        val representative = seasonalBloomEstimateRepository
            .findByAttractionIdAndBaseDate(attractionId, baseDate)
            .minWithOrNull(BloomEstimateOrdering.REPRESENTATIVE_FIRST)
            ?: return null
        return representative.toBanner(baseDate)
    }

    /** 명소에 연결된 스팟만 혼잡도·예보 구역을 가진다. 개화 배너와 같은 추정으로 꽃을 볼 수 있는 날을 판단한다. */
    private fun resolveVisitTiming(spot: Spot, bloom: BloomBanner?): VisitTimingResponse? {
        val attractionId = spot.attractionId ?: return null
        val visitBloom = bloom?.let { VisitTimingBloom(it.status, it.peakStartDate, it.peakEndDate) }
        return visitTimingService.resolve(attractionId, spot.latitude, spot.longitude, visitBloom)
    }

    /**
     * 명소에 연결된 스팟만 운영 정보를 가진다. 받아 둔 항목이 하나도 없으면 화면에서 영역을 빼도록 null 을 준다.
     *
     * 화면(SCR-025a)에는 쉬는 날 줄이 없어 운영 시간 끝에 붙인다. 이용시간에 이미 같은 문구가 있으면 붙이지 않는다.
     */
    private fun resolveOperatingInfo(spot: Spot): OperatingInfo? {
        val attractionId = spot.attractionId ?: return null
        val info = attractionOperatingInfoRepository.findByAttractionId(attractionId) ?: return null
        val closedDays = info.closedDays?.takeUnless { info.operatingHours?.contains(it) == true }
        val operatingHours = listOfNotNull(info.operatingHours, closedDays?.let { "$CLOSED_DAYS_LABEL$it" })
            .joinToString("\n")
            .ifEmpty { null }
        if (operatingHours == null && info.admissionFee == null && info.parking == null) return null
        return OperatingInfo(
            operatingHours = operatingHours,
            admissionFee = info.admissionFee,
            parking = info.parking,
        )
    }

    private fun resolveFavorite(spotId: Long, userId: Long?): FavoriteState {
        val favorite = userId?.let { spotFavoriteRepository.findByUserIdAndSpotId(it, spotId) }
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
        private const val CLOSED_DAYS_LABEL = "쉬는 날: "
    }
}
