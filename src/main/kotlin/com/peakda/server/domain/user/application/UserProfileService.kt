package com.peakda.server.domain.user.application

import com.peakda.server.common.storage.ProfileImageUrlResolver
import com.peakda.server.domain.spot.application.SpotRecordResponseAssembler
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.presentation.response.FavoriteCategoryResponse
import com.peakda.server.domain.user.presentation.response.UserProfileResponse
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserFavoriteCategoryRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 타인 프로필 조회(SCR-024h/i) — 통계(기록·팔로워·팔로잉 수), 관심 꽃(읽기전용), 최근 기록 그리드,
 * 현재 로그인 사용자 기준 팔로우 상태를 한 번에 조합한다.
 */
@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val userFavoriteCategoryRepository: UserFavoriteCategoryRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordResponseAssembler: SpotRecordResponseAssembler,
    private val profileImageUrlResolver: ProfileImageUrlResolver,
) {

    @Transactional(readOnly = true)
    fun getProfile(targetUserId: Long, currentUserId: Long): UserProfileResponse {
        val user = userRepository.findById(targetUserId).orElseThrow { UserNotFoundException() }

        val recordsPage = spotRecordRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            targetUserId,
            SpotRecordStatus.PUBLISHED,
            PageRequest.of(0, GRID_PREVIEW_SIZE),
        )
        val favoriteCategories = userFavoriteCategoryRepository.findByIdUserId(targetUserId).map { it.category }

        return UserProfileResponse(
            userId = targetUserId,
            nickname = user.nickname,
            profileImageUrl = profileImageUrlResolver.resolve(user.profileImageUrl),
            stats = UserProfileResponse.Stats(
                recordCount = recordsPage.totalElements,
                followerCount = followRepository.countByFollowingId(targetUserId),
                followingCount = followRepository.countByFollowerId(targetUserId),
            ),
            favoriteCategories = FavoriteCategoryResponse.of(favoriteCategories),
            recordPreview = spotRecordResponseAssembler.assembleSummaries(recordsPage.content),
            following = currentUserId != targetUserId &&
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId),
        )
    }

    companion object {
        private const val GRID_PREVIEW_SIZE = 6
    }
}
