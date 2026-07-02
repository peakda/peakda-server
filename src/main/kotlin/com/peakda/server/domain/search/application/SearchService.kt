package com.peakda.server.domain.search.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.storage.ProfileImageUrlResolver
import com.peakda.server.domain.search.presentation.response.SpotSearchItem
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse
import com.peakda.server.domain.search.presentation.response.TrendingSpotsResponse.TrendingSpotItem
import com.peakda.server.domain.search.presentation.response.UserSearchItem
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.PageRequest as SpringPageRequest

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
) {

    @Transactional(readOnly = true)
    fun searchSpots(query: String, pageRequest: PageRequest): PageResponse<SpotSearchItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyPage(pageRequest)
        val pageable = pageRequest.toPageable(Sort.by(Sort.Direction.ASC, "name"))
        return spotRepository.findByVisibleTrueAndNameContainingIgnoreCase(q, pageable)
            .map { it.toSearchItem() }
            .toPageResponse()
    }

    @Transactional(readOnly = true)
    fun searchUsers(query: String, pageRequest: PageRequest): PageResponse<UserSearchItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyPage(pageRequest)
        val pageable = pageRequest.toPageable(Sort.by(Sort.Direction.ASC, "nickname"))
        return userRepository.findByStatusAndNicknameContainingIgnoreCase(UserStatus.ACTIVE, q, pageable)
            .map { it.toSearchItem() }
            .toPageResponse()
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

    private fun Spot.toSearchItem() = SpotSearchItem(
        spotId = requireNotNull(id),
        type = type,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
    )

    private fun Spot.toTrendingItem(favoriteCount: Long) = TrendingSpotItem(
        spotId = requireNotNull(id),
        type = type,
        name = name,
        latitude = latitude,
        longitude = longitude,
        favoriteCount = favoriteCount,
    )

    private fun User.toSearchItem() = UserSearchItem(
        userId = requireNotNull(id),
        nickname = nickname,
        profileImageUrl = profileImageUrlResolver.resolve(profileImageUrl),
    )

    companion object {
        private const val TRENDING_LIMIT = 10
    }
}
