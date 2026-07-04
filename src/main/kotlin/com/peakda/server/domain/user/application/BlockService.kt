package com.peakda.server.domain.user.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.storage.ProfileImageUrlResolver
import com.peakda.server.domain.user.exception.SelfBlockNotAllowedException
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.presentation.response.BlockedUserResponse
import com.peakda.server.domain.user.repository.BlockRepository
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사용자 차단(SCR-024h, P2-4). 차단 시 서로의 팔로우 관계도 함께 정리한다.
 */
@Service
@Transactional
class BlockService(
    private val blockRepository: BlockRepository,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val profileImageUrlResolver: ProfileImageUrlResolver,
) {

    fun block(blockerId: Long, targetUserId: Long) {
        if (blockerId == targetUserId) throw SelfBlockNotAllowedException()
        requireUserExists(targetUserId)
        // ON CONFLICT DO NOTHING — 이미 차단 중이면 무시되어 동시 요청에서도 단일 행이 보장된다.
        blockRepository.insertIfAbsent(blockerId, targetUserId)
        followRepository.deleteByFollowerIdAndFollowingId(blockerId, targetUserId)
        followRepository.deleteByFollowerIdAndFollowingId(targetUserId, blockerId)
    }

    fun unblock(blockerId: Long, targetUserId: Long) {
        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetUserId)
    }

    /** 사용자가 맺은 모든 차단 관계(양방향)를 삭제한다. 계정 탈퇴 시 사용. */
    fun deleteAllByUser(userId: Long) {
        blockRepository.deleteAllByUserId(userId)
    }

    @Transactional(readOnly = true)
    fun list(blockerId: Long, pageRequest: PageRequest): PageResponse<BlockedUserResponse> {
        val page = blockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId, pageRequest.toPageable())
        val usersById = userRepository.findAllById(page.content.map { it.blockedId }).associateBy { requireNotNull(it.id) }
        val responses = page.content.mapNotNull { block ->
            usersById[block.blockedId]?.let { user ->
                BlockedUserResponse(
                    userId = requireNotNull(user.id),
                    nickname = user.nickname,
                    profileImageUrl = profileImageUrlResolver.resolve(user.profileImageUrl),
                    blockedAt = block.createdAt,
                )
            }
        }
        return PageResponse(
            content = responses,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
        )
    }

    private fun requireUserExists(userId: Long) {
        if (!userRepository.existsById(userId)) throw UserNotFoundException()
    }
}
