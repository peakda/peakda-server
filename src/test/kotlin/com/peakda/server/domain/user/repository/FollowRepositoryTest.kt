package com.peakda.server.domain.user.repository

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

class FollowRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var followRepository: FollowRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanUp() {
        followRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    @Transactional
    fun `팔로잉 수와 목록은 존재하지 않거나 탈퇴한 상대를 똑같이 제외한다`() {
        val me = saveUser("me").id!!
        val active = saveUser("active")
        val withdrawn = saveUser("withdrawn").apply { withdraw() }
        followRepository.insertIfAbsent(me, active.id!!)
        followRepository.insertIfAbsent(me, withdrawn.id!!)
        followRepository.insertIfAbsent(me, MISSING_USER_ID)

        val page = followRepository.findFollowings(me, PageRequest.of(0, 20))

        assertThat(page.content.map { it.followingId }).containsExactly(active.id)
        assertThat(page.totalElements).isEqualTo(1L)
        assertThat(followRepository.countFollowings(me)).isEqualTo(1L)
    }

    @Test
    @Transactional
    fun `팔로워 수와 목록, 검색용 팔로워 수는 존재하지 않거나 탈퇴한 팔로워를 똑같이 제외한다`() {
        val me = saveUser("me").id!!
        val active = saveUser("active")
        val withdrawn = saveUser("withdrawn").apply { withdraw() }
        followRepository.insertIfAbsent(active.id!!, me)
        followRepository.insertIfAbsent(withdrawn.id!!, me)
        followRepository.insertIfAbsent(MISSING_USER_ID, me)

        val page = followRepository.findFollowers(me, PageRequest.of(0, 20))

        assertThat(page.content.map { it.followerId }).containsExactly(active.id)
        assertThat(page.totalElements).isEqualTo(1L)
        assertThat(followRepository.countFollowers(me)).isEqualTo(1L)
        assertThat(followRepository.countByFollowingIdIn(listOf(me)).single().followerCount).isEqualTo(1L)
    }

    @Test
    @Transactional
    fun `탈퇴 익명화한 사용자는 닉네임 길이 제약을 통과해 저장된다`() {
        val user = saveUser("탈퇴할사용자")

        user.withdraw()
        userRepository.saveAndFlush(user)

        val id = requireNotNull(user.id)
        val saved = userRepository.findById(id).orElseThrow()
        assertThat(saved.status).isEqualTo(UserStatus.DEACTIVATED)
        assertThat(saved.nickname).isEqualTo("탈퇴_${id.toString(36)}")
    }

    private fun saveUser(nickname: String): User =
        userRepository.saveAndFlush(
            User.create(
                provider = OAuth2LoginType.KAKAO,
                providerId = "provider-$nickname",
                nickname = nickname,
                email = null,
                profileImageUrl = null,
            ),
        )

    companion object {
        private const val MISSING_USER_ID = 999_999L
    }
}
