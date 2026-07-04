package com.peakda.server.domain.user.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.storage.ProfileImageUrlResolver
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.user.entity.Block
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.exception.SelfBlockNotAllowedException
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.repository.BlockRepository
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class BlockServiceTest {

    private val blockRepository = mock(BlockRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val followRepository = mock(FollowRepository::class.java)
    private val profileImageUrlResolver = mock(ProfileImageUrlResolver::class.java)

    private val service = BlockService(blockRepository, userRepository, followRepository, profileImageUrlResolver)

    @Test
    fun `자기 자신은 차단할 수 없다`() {
        assertThatThrownBy { service.block(BLOCKER_ID, BLOCKER_ID) }
            .isInstanceOf(SelfBlockNotAllowedException::class.java)
        verify(blockRepository, never()).insertIfAbsent(BLOCKER_ID, BLOCKER_ID)
    }

    @Test
    fun `대상 사용자가 없으면 UserNotFoundException 이다`() {
        `when`(userRepository.existsById(BLOCKED_ID)).thenReturn(false)

        assertThatThrownBy { service.block(BLOCKER_ID, BLOCKED_ID) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `차단하면 서로의 팔로우 관계도 함께 해제된다`() {
        `when`(userRepository.existsById(BLOCKED_ID)).thenReturn(true)

        service.block(BLOCKER_ID, BLOCKED_ID)

        verify(blockRepository).insertIfAbsent(BLOCKER_ID, BLOCKED_ID)
        verify(followRepository).deleteByFollowerIdAndFollowingId(BLOCKER_ID, BLOCKED_ID)
        verify(followRepository).deleteByFollowerIdAndFollowingId(BLOCKED_ID, BLOCKER_ID)
    }

    @Test
    fun `차단 해제는 저장소에 위임한다`() {
        service.unblock(BLOCKER_ID, BLOCKED_ID)

        verify(blockRepository).deleteByBlockerIdAndBlockedId(BLOCKER_ID, BLOCKED_ID)
    }

    @Test
    fun `차단 목록은 사용자 정보를 채워 반환하고 존재하지 않는 사용자는 제외한다`() {
        val pageable = SpringPageRequest.of(0, 20)
        val block = block(BLOCKED_ID)
        `when`(blockRepository.findByBlockerIdOrderByCreatedAtDesc(BLOCKER_ID, pageable))
            .thenReturn(PageImpl(listOf(block), pageable, 1))
        `when`(userRepository.findAllById(listOf(BLOCKED_ID))).thenReturn(listOf(user(BLOCKED_ID, "불편러")))
        `when`(profileImageUrlResolver.resolve(null)).thenReturn(null)

        val response = service.list(BLOCKER_ID, PageRequest(page = 0, size = 20))

        assertThat(response.content).hasSize(1)
        assertThat(response.content.first().userId).isEqualTo(BLOCKED_ID)
        assertThat(response.content.first().nickname).isEqualTo("불편러")
    }

    private fun block(blockedId: Long): Block {
        val block = Block(blockerId = BLOCKER_ID, blockedId = blockedId)
        ReflectionTestUtils.setField(block, "id", 1L)
        ReflectionTestUtils.setField(block, "createdAt", Instant.now())
        return block
    }

    private fun user(id: Long, nickname: String): User {
        val user = User(provider = OAuth2LoginType.KAKAO, providerId = "p-$id", nickname = nickname)
        ReflectionTestUtils.setField(user, "id", id)
        return user
    }

    companion object {
        private const val BLOCKER_ID = 1L
        private const val BLOCKED_ID = 2L
    }
}
