package com.peakda.server.domain.user.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.spot.application.SpotRecordResponseAssembler
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserFavoriteCategory
import com.peakda.server.domain.user.entity.UserFavoriteCategoryId
import com.peakda.server.domain.user.exception.UserNotFoundException
import com.peakda.server.domain.user.repository.FollowRepository
import com.peakda.server.domain.user.repository.UserFavoriteCategoryRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

class UserProfileServiceTest {

    private val userRepository = mock(UserRepository::class.java)
    private val followRepository = mock(FollowRepository::class.java)
    private val userFavoriteCategoryRepository = mock(UserFavoriteCategoryRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordResponseAssembler = mock(SpotRecordResponseAssembler::class.java)
    private val objectKeyUrlResolver = mock(ObjectKeyUrlResolver::class.java)

    private val service = UserProfileService(
        userRepository,
        followRepository,
        userFavoriteCategoryRepository,
        spotRecordRepository,
        spotRecordResponseAssembler,
        objectKeyUrlResolver,
    )

    @Test
    fun `대상 사용자가 없으면 UserNotFoundException 이다`() {
        `when`(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty())

        assertThatThrownBy { service.getProfile(TARGET_ID, VIEWER_ID) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `통계·관심꽃·기록그리드·팔로우 상태를 조합한다`() {
        val user = user(TARGET_ID, "벚꽃러버", "profile-images/42/main.jpg")
        `when`(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(user))
        `when`(objectKeyUrlResolver.resolve("profile-images/42/main.jpg")).thenReturn("https://cdn/main.jpg")

        val pageable = PageRequest.of(0, 6)
        val record = record(1L)
        `when`(spotRecordRepository.findByUserIdAndStatusOrderByCreatedAtDesc(TARGET_ID, SpotRecordStatus.PUBLISHED, pageable))
            .thenReturn(PageImpl(listOf(record), pageable, 24))
        `when`(spotRecordResponseAssembler.assembleSummaries(listOf(record))).thenReturn(listOf(summary(1L)))

        `when`(userFavoriteCategoryRepository.findByIdUserId(TARGET_ID))
            .thenReturn(listOf(favoriteCategory(TARGET_ID, BloomCategory.CHERRY)))

        `when`(followRepository.countByFollowingId(TARGET_ID)).thenReturn(1280L)
        `when`(followRepository.countByFollowerId(TARGET_ID)).thenReturn(312L)
        `when`(followRepository.existsByFollowerIdAndFollowingId(VIEWER_ID, TARGET_ID)).thenReturn(true)

        val response = service.getProfile(TARGET_ID, VIEWER_ID)

        assertThat(response.userId).isEqualTo(TARGET_ID)
        assertThat(response.nickname).isEqualTo("벚꽃러버")
        assertThat(response.profileImageUrl).isEqualTo("https://cdn/main.jpg")
        assertThat(response.stats.recordCount).isEqualTo(24)
        assertThat(response.stats.followerCount).isEqualTo(1280)
        assertThat(response.stats.followingCount).isEqualTo(312)
        assertThat(response.favoriteCategories.categories).extracting<BloomCategory> { it.category }
            .containsExactly(BloomCategory.CHERRY)
        assertThat(response.recordPreview).extracting<Long> { it.id }.containsExactly(1L)
        assertThat(response.following).isTrue()
    }

    @Test
    fun `본인 프로필을 조회하면 following 은 항상 false 다`() {
        val user = user(TARGET_ID, "나", null)
        `when`(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(user))
        `when`(objectKeyUrlResolver.resolve(null)).thenReturn(null)
        val pageable = PageRequest.of(0, 6)
        `when`(spotRecordRepository.findByUserIdAndStatusOrderByCreatedAtDesc(TARGET_ID, SpotRecordStatus.PUBLISHED, pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0))
        `when`(spotRecordResponseAssembler.assembleSummaries(emptyList())).thenReturn(emptyList())
        `when`(userFavoriteCategoryRepository.findByIdUserId(TARGET_ID)).thenReturn(emptyList())
        `when`(followRepository.countByFollowingId(TARGET_ID)).thenReturn(0L)
        `when`(followRepository.countByFollowerId(TARGET_ID)).thenReturn(0L)

        val response = service.getProfile(TARGET_ID, TARGET_ID)

        assertThat(response.following).isFalse()
    }

    // --- fixtures ---

    private fun user(id: Long, nickname: String, profileImageUrl: String?): User {
        val user = User(
            provider = OAuth2LoginType.KAKAO,
            providerId = "p-$id",
            nickname = nickname,
            profileImageUrl = profileImageUrl,
        )
        ReflectionTestUtils.setField(user, "id", id)
        return user
    }

    private fun record(id: Long): SpotRecord {
        val record = SpotRecord(spotId = 100L, userId = TARGET_ID, status = SpotRecordStatus.PUBLISHED)
        ReflectionTestUtils.setField(record, "id", id)
        return record
    }

    private fun summary(id: Long) = SpotRecordSummaryResponse(
        id = id,
        spotId = 100L,
        spotName = "남산",
        user = SpotRecordResponse.UserSummary(id = TARGET_ID, nickname = "벚꽃러버", profileImageUrl = null),
        visitedDate = null,
        bloomStage = null,
        memo = null,
        plants = emptyList(),
        coverPhoto = null,
        status = SpotRecordStatus.PUBLISHED,
        publishedAt = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    private fun favoriteCategory(userId: Long, category: BloomCategory) =
        UserFavoriteCategory(UserFavoriteCategoryId(userId, category))

    companion object {
        private const val TARGET_ID = 42L
        private const val VIEWER_ID = 7L
    }
}
