package com.peakda.server.domain.search.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.domain.search.presentation.response.SpotSearchItem
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse.TrendingSpotItem
import com.peakda.server.domain.search.presentation.response.UserSearchItem
import com.peakda.server.domain.seasonal.application.BloomBaseDateResolver
import com.peakda.server.domain.seasonal.application.BloomEstimateOrdering
import com.peakda.server.domain.seasonal.application.BloomStageStatusMapper
import com.peakda.server.domain.seasonal.application.LocalSpotBloomResolver
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.application.SpotThumbnailResolver
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.BloomBadge
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.user.application.ProfileImageUrlResolver
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/**
 * 검색 도메인(SCR-021~021e) — 스팟/사용자 검색과 트렌딩 스팟 목록.
 *
 * 최근 검색어는 서버에 저장하지 않는다(결정 H, 디바이스 로컬). 트렌딩은 검색어 로그가 없어
 * 찜 수를 대체 인기 신호로 쓴다.
 */
@Service
class SearchService(
    private val spotRepository: SpotRepository,
    private val userRepository: UserRepository,
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val profileImageUrlResolver: ProfileImageUrlResolver,
    private val followRepository: FollowRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val bloomBaseDateResolver: BloomBaseDateResolver,
    private val localSpotBloomResolver: LocalSpotBloomResolver,
    private val spotThumbnailResolver: SpotThumbnailResolver,
) {

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun searchSpots(userId: Long?, query: String, pageRequest: PageRequest, category: BloomCategory? = null): PageResponse<SpotSearchItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyPage(pageRequest)

        val pageable = pageRequest.toPageable(Sort.by(Sort.Direction.ASC, "name"))
        val page = if (category == null) {
            val spotPage = spotRepository.findByVisibleTrueAndNameContainingIgnoreCase(q, pageable)
            val badges = bloomBySpot(spotPage.content)
            spotPage.map { spot -> spot to badges[spot.id] }
        } else {
            findCategoryPage(q, pageable, category)
        }
        val spots = page.content.map { it.first }
        val spotIds = spots.mapNotNull { it.id }
        val favoritesBySpot = if (userId == null || spotIds.isEmpty()) emptyMap() else {
            spotFavoriteRepository.findByUserIdAndSpotIdIn(userId, spotIds).associateBy { it.spotId }
        }
        val thumbnailBySpot = spotThumbnailResolver.resolve(spots)
        return page.map { (spot, bloom) ->
            val id = requireNotNull(spot.id)
            spot.toSearchItem(thumbnailBySpot[id], bloom, favoritesBySpot[id])
        }.toPageResponse()
    }

    private fun findCategoryPage(
        query: String,
        pageable: Pageable,
        category: BloomCategory,
    ): Page<Pair<Spot, BloomBadge?>> {
        val baseDate by lazy(bloomBaseDateResolver::currentBaseDate)
        val observedFrom by lazy(localSpotBloomResolver::oldestFreshObservationDate)
        val pageMatches = linkedMapOf<Long, BloomBadge>()
        val pageRange = pageable.offset until (pageable.offset + pageable.pageSize)
        var total = 0L
        var position: ScrollPosition = ScrollPosition.keyset()

        // 정확한 total은 모든 후보 배치를 검사하되, 페이지에 필요한 일치 항목만 보관한다.
        while (true) {
            val window = spotRepository.findFirst128ByVisibleTrueAndNameContainingIgnoreCaseOrderByNameAscIdAsc(query, position)
            val attractionIds = window.content.filter { it.type == SpotType.ATTRACTION }.mapNotNull { it.attractionId }
            val attractionStatuses = if (attractionIds.isEmpty()) emptyMap() else {
                baseDate?.let {
                    seasonalBloomEstimateRepository.findStatusesByBaseDateAndAttractionIdInAndBloomCategory(it, attractionIds, category)
                        .associate { row -> row.attractionId to row.status }
                }.orEmpty()
            }
            val localIds = window.content.filter { it.type == SpotType.LOCAL }.map { it.id }
            val localStages = if (localIds.isEmpty()) emptyMap() else {
                observedFrom?.let {
                    spotRecordRepository.findLatestTaggedBloomStages(localIds, SpotRecordStatus.PUBLISHED.name, category.name, it)
                        .associate { row -> row.spotId to BloomStage.valueOf(row.bloomStage) }
                }.orEmpty()
            }
            for (candidate in window) {
                val status = when (candidate.type) {
                    SpotType.ATTRACTION -> attractionStatuses[candidate.attractionId]
                    SpotType.LOCAL -> localStages[candidate.id]?.let(BloomStageStatusMapper::toStatus)
                } ?: continue
                if (total in pageRange) {
                    pageMatches[candidate.id] = BloomBadge(category, category.displayName, status)
                }
                total++
            }
            if (window.isLast || window.isEmpty) break
            position = window.positionAt(window.size() - 1)
        }

        val spotsById = if (pageMatches.isEmpty()) emptyMap() else {
            spotRepository.findAllById(pageMatches.keys).associateBy { requireNotNull(it.id) }
        }
        val content = pageMatches.map { (id, badge) -> spotsById.getValue(id) to badge }
        return PageImpl(content, pageable, total)
    }

    @Transactional(readOnly = true)
    fun searchUsers(userId: Long, query: String, pageRequest: PageRequest): PageResponse<UserSearchItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyPage(pageRequest)
        val pageable = pageRequest.toPageable(Sort.by(Sort.Direction.ASC, "nickname"))
        val page = userRepository.findByStatusAndNicknameContainingIgnoreCase(UserStatus.ACTIVE, q, pageable)
        val users = page.content
        val userIds = users.mapNotNull { it.id }
        val followingIds = if (userIds.isEmpty()) emptySet() else {
            followRepository.findFollowingTargetIds(userId, userIds).toSet()
        }
        val recordCountByUser = if (userIds.isEmpty()) emptyMap() else {
            spotRecordRepository.countByUserIdInAndStatus(userIds, SpotRecordStatus.PUBLISHED)
                .associate { it.userId to it.recordCount }
        }
        val followerCountByUser = if (userIds.isEmpty()) emptyMap() else {
            followRepository.countByFollowingIdIn(userIds).associate { it.userId to it.followerCount }
        }
        return page.map { user ->
            val id = requireNotNull(user.id)
            user.toSearchItem(
                following = id in followingIds,
                recordCount = recordCountByUser[id] ?: 0L,
                followerCount = followerCountByUser[id] ?: 0L,
            )
        }.toPageResponse()
    }

    @Transactional(readOnly = true)
    fun trending(): TrendingSpotsResponse {
        val counts = spotFavoriteRepository.findTrendingSpotIds(SpringPageRequest.of(0, TRENDING_LIMIT))
        val spotsById = spotRepository.findAllById(counts.map { it.spotId }).associateBy { requireNotNull(it.id) }
        val items = counts.mapNotNull { count -> spotsById[count.spotId]?.toTrendingItem(count.favoriteCount) }
        return TrendingSpotsResponse(items)
    }

    private fun <T> emptyPage(pageRequest: PageRequest): PageResponse<T> = PageResponse(
        content = emptyList(),
        page = pageRequest.page,
        size = pageRequest.size,
        totalElements = 0L,
        totalPages = 0,
        hasNext = false,
    )

    private fun Spot.toSearchItem(thumbnailUrl: String?, bloom: BloomBadge?, favorite: SpotFavorite?) = SpotSearchItem(
        spotId = requireNotNull(id),
        type = type,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
        thumbnailUrl = thumbnailUrl,
        bloom = bloom,
        favorited = favorite != null,
        notifyEnabled = favorite?.notifyEnabled ?: false,
    )

    private fun Spot.toTrendingItem(favoriteCount: Long) = TrendingSpotItem(
        spotId = requireNotNull(id),
        type = type,
        name = name,
        latitude = latitude,
        longitude = longitude,
        favoriteCount = favoriteCount,
    )

    private fun User.toSearchItem(following: Boolean, recordCount: Long, followerCount: Long) = UserSearchItem(
        userId = requireNotNull(id),
        nickname = nickname,
        profileImageUrl = profileImageUrlResolver.thumbnailUrl(profileImageUrl),
        following = following,
        recordCount = recordCount,
        followerCount = followerCount,
    )

    private fun bloomBySpot(spots: Collection<Spot>): Map<Long, BloomBadge> {
        val attractionIdBySpot = spots
            .filter { it.type == SpotType.ATTRACTION }
            .mapNotNull { spot -> spot.attractionId?.let { requireNotNull(spot.id) to it } }
        val attractionBadges = if (attractionIdBySpot.isEmpty()) emptyMap() else {
            val baseDate = bloomBaseDateResolver.currentBaseDate()
            if (baseDate == null) emptyMap() else {
                val estimates = seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(
                    baseDate,
                    attractionIdBySpot.map { it.second },
                )
                val byAttraction = estimates
                    .groupBy { it.attractionId }
                    .mapValues { (_, rows) -> rows.minWith(BloomEstimateOrdering.REPRESENTATIVE_FIRST).toBadge() }
                attractionIdBySpot.mapNotNull { (spotId, attractionId) -> byAttraction[attractionId]?.let { spotId to it } }.toMap()
            }
        }

        val localSpotIds = spots.filter { it.type == SpotType.LOCAL }.mapNotNull { it.id }
        if (localSpotIds.isEmpty()) return attractionBadges
        val records = spotRecordRepository.findBySpotIdInAndStatus(localSpotIds, SpotRecordStatus.PUBLISHED)
        // 동네형은 스팟당 대표 뱃지 1건만 노출한다 — 가장 최근 관측 신호의 첫 건.
        val localBadges = localSpotBloomResolver.resolve(records)
            .mapNotNull { (spotId, signals) ->
                signals.firstOrNull()
                    ?.let { spotId to BloomBadge(it.category, it.category.displayName, it.status) }
            }
            .toMap()
        return attractionBadges + localBadges
    }

    private fun SeasonalBloomEstimate.toBadge() = BloomBadge(bloomCategory, bloomCategory.displayName, status)

    companion object {
        private const val TRENDING_LIMIT = 10
    }
}
