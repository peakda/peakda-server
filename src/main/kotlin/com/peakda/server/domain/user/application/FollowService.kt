package com.peakda.server.domain.user.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.storage.ProfileImageUrlResolver
import com.peakda.server.domain.user.entity.Follow
import com.peakda.server.domain.user.exception.SelfFollowNotAllowedException
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.presentation.response.FollowSummaryResponse
import com.peakda.server.domain.user.presentation.response.FollowUserResponse
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val profileImageUrlResolver: ProfileImageUrlResolver,
) {

    fun follow(followerId: Long, targetUserId: Long) {
        if (followerId == targetUserId) throw SelfFollowNotAllowedException()
        requireUserExists(targetUserId)
        // ON CONFLICT DO NOTHING — 이미 팔로우 중이면 무시되어 동시 요청에서도 단일 행이 보장된다.
        followRepository.insertIfAbsent(followerId, targetUserId)
    }

    fun unfollow(followerId: Long, targetUserId: Long) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, targetUserId)
    }

    /** 사용자가 맺은 모든 팔로우 관계(양방향)를 삭제한다. 계정 탈퇴 시 사용. */
    fun deleteAllByUser(userId: Long) {
        followRepository.deleteAllByUserId(userId)
    }

    @Transactional(readOnly = true)
    fun getFollowers(
        targetUserId: Long,
        currentUserId: Long,
        pageRequest: PageRequest,
    ): PageResponse<FollowUserResponse> {
        requireUserExists(targetUserId)
        val page = followRepository.findByFollowingIdOrderByCreatedAtDesc(targetUserId, pageRequest.toPageable())
        return page.toUserResponses(currentUserId) { it.followerId }
    }

    @Transactional(readOnly = true)
    fun getFollowings(
        targetUserId: Long,
        currentUserId: Long,
        pageRequest: PageRequest,
    ): PageResponse<FollowUserResponse> {
        requireUserExists(targetUserId)
        val page = followRepository.findByFollowerIdOrderByCreatedAtDesc(targetUserId, pageRequest.toPageable())
        return page.toUserResponses(currentUserId) { it.followingId }
    }

    @Transactional(readOnly = true)
    fun getSummary(targetUserId: Long, currentUserId: Long): FollowSummaryResponse {
        requireUserExists(targetUserId)
        return FollowSummaryResponse(
            userId = targetUserId,
            followerCount = followRepository.countByFollowingId(targetUserId),
            followingCount = followRepository.countByFollowerId(targetUserId),
            following = currentUserId != targetUserId &&
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId),
        )
    }

    private fun requireUserExists(userId: Long) {
        if (!userRepository.existsById(userId)) throw UserNotFoundException()
    }

    /**
     * Follow 페이지의 각 행에서 [userIdSelector] 로 상대 사용자 id 를 뽑아 사용자 정보를 채우고,
     * 현재 로그인 사용자([currentUserId]) 기준의 팔로우 여부를 한 번의 쿼리로 계산한다.
     */
    private fun Page<Follow>.toUserResponses(
        currentUserId: Long,
        userIdSelector: (Follow) -> Long,
    ): PageResponse<FollowUserResponse> {
        val userIds = content.map(userIdSelector)
        val usersById = userRepository.findAllById(userIds).associateBy { requireNotNull(it.id) }
        val followingTargetIds =
            if (userIds.isEmpty()) emptySet()
            else followRepository.findFollowingTargetIds(currentUserId, userIds).toSet()

        val responses = content.mapNotNull { follow ->
            val userId = userIdSelector(follow)
            usersById[userId]?.let { user ->
                FollowUserResponse(
                    userId = userId,
                    nickname = user.nickname,
                    profileImageUrl = profileImageUrlResolver.resolve(user.profileImageUrl),
                    following = userId in followingTargetIds,
                    followedAt = follow.createdAt,
                )
            }
        }
        return PageResponse(
            content = responses,
            page = number,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = hasNext(),
        )
    }
}
