package com.peakda.server.domain.search.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.search.presentation.response.SpotSearchItem
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse.TrendingSpotItem
import com.peakda.server.domain.search.presentation.response.UserSearchItem
import com.peakda.server.domain.seasonal.application.BloomStageStatusMapper
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.application.SpotThumbnailResolver
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.BloomBadge
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.PageRequest as SpringPageRequest
import java.time.LocalDate
import java.time.ZoneOffset

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
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
    private val followRepository: FollowRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val plantRepository: PlantRepository,
    private val spotThumbnailResolver: SpotThumbnailResolver,
) {

    @Transactional(readOnly = true)
    fun searchSpots(userId: Long, query: String, pageRequest: PageRequest, category: BloomCategory? = null): PageResponse<SpotSearchItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyPage(pageRequest)
        val pageable = pageRequest.toPageable(Sort.by(Sort.Direction.ASC, "name"))
        val page = spotRepository.findByVisibleTrueAndNameContainingIgnoreCase(q, pageable)
        val spots = page.content
        val spotIds = spots.mapNotNull { it.id }
        val favoritesBySpot = if (spotIds.isEmpty()) emptyMap() else {
            spotFavoriteRepository.findByUserIdAndSpotIdIn(userId, spotIds).associateBy { it.spotId }
        }
        val bloomBySpot = bloomBySpot(spots, category)
        val thumbnailBySpot = spotThumbnailResolver.resolve(spots)
        val content = spots.mapNotNull { spot ->
            val id = spot.id ?: return@mapNotNull null
            val bloom = bloomBySpot[id]
            if (category != null && bloom == null) return@mapNotNull null
            spot.toSearchItem(thumbnailBySpot[id], bloom, favoritesBySpot[id])
        }
        return if (category == null) {
            page.map { spot ->
                val id = requireNotNull(spot.id)
                spot.toSearchItem(thumbnailBySpot[id], bloomBySpot[id], favoritesBySpot[id])
            }.toPageResponse()
        } else {
            PageResponse(
                content = content,
                page = page.number,
                size = page.size,
                totalElements = content.size.toLong(),
                totalPages = if (content.isEmpty()) 0 else 1,
                hasNext = false,
            )
        }
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
        profileImageUrl = objectKeyUrlResolver.resolve(profileImageUrl),
        following = following,
        recordCount = recordCount,
        followerCount = followerCount,
    )

    private fun bloomBySpot(spots: Collection<Spot>, category: BloomCategory?): Map<Long, BloomBadge> {
        val attractionIdBySpot = spots
            .filter { it.type == SpotType.ATTRACTION }
            .mapNotNull { spot -> spot.attractionId?.let { requireNotNull(spot.id) to it } }
        val attractionBadges = if (attractionIdBySpot.isEmpty()) emptyMap() else {
            val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate()
            if (baseDate == null) emptyMap() else {
                val estimates = if (category == null) {
                    seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(
                        baseDate,
                        attractionIdBySpot.map { it.second },
                    )
                } else {
                    seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdInAndBloomCategory(
                        baseDate,
                        attractionIdBySpot.map { it.second },
                        category,
                    )
                }
                val byAttraction = estimates
                    .filter { it.status != BloomStatus.ENDED }
                    .groupBy { it.attractionId }
                    .mapValues { (_, rows) -> rows.minWith(compareBy({ statusRank(it.status) }, { -it.confidence })).toBadge() }
                attractionIdBySpot.mapNotNull { (spotId, attractionId) -> byAttraction[attractionId]?.let { spotId to it } }.toMap()
            }
        }

        val localSpotIds = spots.filter { it.type == SpotType.LOCAL }.mapNotNull { it.id }
        if (localSpotIds.isEmpty()) return attractionBadges
        val records = spotRecordRepository.findBySpotIdInAndStatus(localSpotIds, SpotRecordStatus.PUBLISHED)
        if (records.isEmpty()) return attractionBadges
        val joins = spotRecordPlantRepository.findByIdSpotRecordIdIn(records.mapNotNull { it.id })
        val categoryByPlant = plantRepository.findAllById(joins.map { it.plantId }.toSet())
            .mapNotNull { plant -> plant.bloomCategory?.let { requireNotNull(plant.id) to it } }
            .toMap()
        val categoriesByRecord = joins
            .mapNotNull { join -> categoryByPlant[join.plantId]?.let { join.spotRecordId to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.toSet() }
        val localBadges = linkedMapOf<Long, BloomBadge>()
        for (record in records.sortedByDescending { it.recordDate }) {
            if (record.spotId in localBadges) continue
            val stage = record.bloomStage ?: continue
            val status = BloomStageStatusMapper.toStatus(stage)
            if (status == BloomStatus.ENDED) continue
            val matched = categoriesByRecord[record.id].orEmpty().firstOrNull { category == null || it == category } ?: continue
            localBadges[record.spotId] = BloomBadge(matched, matched.displayName, status)
        }
        return attractionBadges + localBadges
    }

    private fun SeasonalBloomEstimate.toBadge() = BloomBadge(bloomCategory, bloomCategory.displayName, status)

    private val SpotRecord.recordDate: LocalDate
        get() = visitedDate ?: createdAt.atZone(ZoneOffset.UTC).toLocalDate()

    private fun statusRank(status: BloomStatus): Int = when (status) {
        BloomStatus.PEAK -> 0
        BloomStatus.STARTED -> 1
        BloomStatus.PREPARING -> 2
        BloomStatus.ENDED -> 3
    }

    companion object {
        private const val TRENDING_LIMIT = 10
    }
}
