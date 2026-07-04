package com.peakda.server.domain.user.application

import com.peakda.server.common.storage.ProfileImageUrlResolver
import com.peakda.server.domain.spot.application.SpotRecordResponseAssembler
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.presentation.response.FavoriteCategoryResponse
import com.peakda.server.domain.user.presentation.response.MyPageResponse
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserFavoriteCategoryRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 마이페이지 집계(SCR-024, P2-5) — 기록·팔로워·팔로잉·찜 수, 관심 꽃, 내 기록 그리드를 한 번에 조합한다.
 */
@Service
class MyPageService(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val userFavoriteCategoryRepository: UserFavoriteCategoryRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordResponseAssembler: SpotRecordResponseAssembler,
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val profileImageUrlResolver: ProfileImageUrlResolver,
) {

    @Transactional(readOnly = true)
    fun getMyPage(userId: Long): MyPageResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }

        val recordsPage = spotRecordRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            userId,
            SpotRecordStatus.PUBLISHED,
            PageRequest.of(0, GRID_PREVIEW_SIZE),
        )
        val favoriteCategories = userFavoriteCategoryRepository.findByIdUserId(userId).map { it.category }

        return MyPageResponse(
            userId = userId,
            nickname = user.nickname,
            profileImageUrl = profileImageUrlResolver.resolve(user.profileImageUrl),
            stats = MyPageResponse.Stats(
                recordCount = recordsPage.totalElements,
                followerCount = followRepository.countByFollowingId(userId),
                followingCount = followRepository.countByFollowerId(userId),
                favoriteSpotCount = spotFavoriteRepository.countByUserId(userId),
            ),
            favoriteCategories = FavoriteCategoryResponse.of(favoriteCategories),
            recordPreview = spotRecordResponseAssembler.assembleSummaries(recordsPage.content),
        )
    }

    companion object {
        private const val GRID_PREVIEW_SIZE = 6
    }
}
