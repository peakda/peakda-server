package com.peakda.server.domain.spot.application

import com.peakda.server.domain.seasonal.application.BloomBaseDateResolver
import com.peakda.server.domain.seasonal.application.BloomEstimateOrdering
import com.peakda.server.domain.seasonal.application.LocalSpotBloomResolver
import com.peakda.server.domain.seasonal.application.peakDurationDaysInclusive
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.BloomBadge
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.SpotPreviewItem
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 핀 클릭 프리뷰(SCR-011d 클러스터 리스트 / SCR-011e 단일 프리뷰) 카드 조립.
 *
 * 두 화면 모두 "지도 핀 → 스팟 카드" 형태로 동일해, spotIds 1건이면 단일 프리뷰,
 * 여러 건이면 클러스터 리스트로 프론트가 그대로 렌더링한다.
 */
@Service
class SpotPreviewService(
    private val spotRepository: SpotRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val bloomBaseDateResolver: BloomBaseDateResolver,
    private val spotRecordRepository: SpotRecordRepository,
    private val localSpotBloomResolver: LocalSpotBloomResolver,
    private val spotThumbnailResolver: SpotThumbnailResolver,
    private val spotRecordPhotoRepository: SpotRecordPhotoRepository,
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
    private val spotFavoriteRepository: SpotFavoriteRepository,
) {

    /** 새 지도 프리뷰 경로. [spotIds] 입력 순서를 결과에도 그대로 보존한다. */
    @Transactional(readOnly = true)
    fun preview(
        spotIds: List<Long>,
        categories: List<BloomCategory>?,
        status: BloomStatus?,
        lat: Double?,
        lng: Double?,
        userId: Long?,
    ): SpotPreviewResponse {
        val distinctIds = spotIds.distinct()
        if (distinctIds.isEmpty()) return SpotPreviewResponse(emptyList())

        val spotsById = spotRepository.findAllById(distinctIds)
            .filter { it.visible }
            .associateBy { requireNotNull(it.id) }
        if (spotsById.isEmpty()) return SpotPreviewResponse(emptyList())

        val badgesBySpot = attractionBadges(spotsById.values, categories) + localBadges(spotsById.values, categories)
        val thumbnailBySpot = spotThumbnailResolver.resolve(spotsById.values)
        val recentPhotoUrlsBySpot = recentPhotoUrls(spotsById.values)
        val recordCounts = spotRecordRepository
            .countBySpotIdInAndStatus(distinctIds, SpotRecordStatus.PUBLISHED)
            .associate { it.spotId to it.recordCount }
        val favoriteBySpot = userId?.let {
            spotFavoriteRepository.findByUserIdAndSpotIdIn(it, distinctIds).associateBy { favorite -> favorite.spotId }
        }.orEmpty()

        val items = distinctIds.mapNotNull { spotId ->
            val spot = spotsById[spotId] ?: return@mapNotNull null
            val badges = badgesBySpot[spotId].orEmpty()
                .filter { status == null || it.status == status }
            if (status != null && badges.isEmpty()) return@mapNotNull null
            val favorite = favoriteBySpot[spotId]
            val thumbnailUrl = thumbnailBySpot[spotId] ?: recentPhotoUrlsBySpot[spotId]?.firstOrNull()
            val photoUrls = (listOfNotNull(thumbnailUrl) + recentPhotoUrlsBySpot[spotId].orEmpty())
                .distinct()
                .take(MAX_PHOTO_COUNT)
            SpotPreviewItem(
                spotId = spotId,
                type = spot.type,
                name = spot.name,
                thumbnailUrl = thumbnailUrl,
                badge = badges.firstOrNull(),
                distanceMeters = distance(lat, lng, spot.latitude, spot.longitude),
                address = spot.address,
                favorited = favorite != null,
                notifyEnabled = favorite?.notifyEnabled ?: false,
                photoUrls = photoUrls,
                recordCount = recordCounts[spotId] ?: 0,
                badges = badges,
            )
        }
        return SpotPreviewResponse(items)
    }

    /** 기존 내부 호출자(큐레이션 등)의 단일 category API 호환용. */
    @Transactional(readOnly = true)
    fun preview(spotIds: List<Long>, category: BloomCategory?, lat: Double?, lng: Double?): SpotPreviewResponse =
        preview(
            spotIds = spotIds,
            categories = category?.let(::listOf),
            status = null,
            lat = lat,
            lng = lng,
            userId = LEGACY_ANONYMOUS_USER_ID,
        )

    /** 명소형 스팟은 ENDED 를 제외하고 상태 우선·신뢰도 순으로 전체 뱃지를 반환한다. */
    private fun attractionBadges(
        spots: Collection<Spot>,
        categories: List<BloomCategory>?,
    ): Map<Long, List<BloomBadge>> {
        val attractionIdBySpot = spots
            .filter { it.type == SpotType.ATTRACTION }
            .mapNotNull { spot -> spot.attractionId?.let { requireNotNull(spot.id) to it } }
        if (attractionIdBySpot.isEmpty()) return emptyMap()

        val baseDate = bloomBaseDateResolver.currentBaseDate() ?: return emptyMap()
        val estimates = seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(
            baseDate,
            attractionIdBySpot.map { it.second },
        )
        val categorySet = categories.orEmpty().toSet()
        val badgeByAttraction = estimates
            .filter { it.status != BloomStatus.ENDED && (categorySet.isEmpty() || it.bloomCategory in categorySet) }
            .groupBy { it.attractionId }
            .mapValues { (_, rows) ->
                rows.sortedWith(BloomEstimateOrdering.REPRESENTATIVE_FIRST)
                    .map { it.toBadge() }
            }

        return attractionIdBySpot.mapNotNull { (spotId, attractionId) ->
            badgeByAttraction[attractionId]?.let { spotId to it }
        }.toMap()
    }

    /** 동네형 스팟은 카테고리별 최근 관측 신호를 그대로 뱃지로 옮긴다. */
    private fun localBadges(
        spots: Collection<Spot>,
        categories: List<BloomCategory>?,
    ): Map<Long, List<BloomBadge>> {
        val localSpotIds = spots.filter { it.type == SpotType.LOCAL }.mapNotNull { it.id }
        if (localSpotIds.isEmpty()) return emptyMap()

        val records = spotRecordRepository.findBySpotIdInAndStatus(localSpotIds, SpotRecordStatus.PUBLISHED)
        val categorySet = categories.orEmpty().toSet()
        return localSpotBloomResolver.resolve(records)
            .mapValues { (_, signals) ->
                signals
                    .filter { categorySet.isEmpty() || it.category in categorySet }
                    .map { BloomBadge(it.category, it.category.displayName, it.status) }
            }
            .filterValues { it.isNotEmpty() }
    }

    /** 최근 게시 기록의 사진을 스팟당 최대 4장 조회한다. */
    private fun recentPhotoUrls(spots: Collection<Spot>): Map<Long, List<String>> {
        val spotIds = spots.mapNotNull { it.id }
        if (spotIds.isEmpty()) return emptyMap()

        return spotRecordPhotoRepository
            .findRecentPhotosBySpotIds(spotIds, SpotRecordStatus.PUBLISHED.name, MAX_PHOTO_COUNT)
            .groupBy { it.spotId }
            .mapValues { (_, photos) -> photos.take(MAX_PHOTO_COUNT).map { spotRecordPhotoUploader.presignedUrlOf(it.objectKey) } }
    }

    private fun SeasonalBloomEstimate.toBadge() = BloomBadge(
        category = bloomCategory,
        displayName = bloomCategory.displayName,
        status = status,
        peakDurationDays = peakDurationDaysInclusive(peakStartDate, peakEndDate),
    )

    /** Haversine 거리(m). 좌표가 없으면 null. */
    private fun distance(lat: Double?, lng: Double?, spotLat: Double, spotLng: Double): Double? {
        if (lat == null || lng == null) return null
        val dLat = Math.toRadians(spotLat - lat)
        val dLng = Math.toRadians(spotLng - lng)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat)) * cos(Math.toRadians(spotLat)) * sin(dLng / 2).pow(2.0)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private const val MAX_PHOTO_COUNT = 4
        private const val LEGACY_ANONYMOUS_USER_ID = 0L
    }
}
