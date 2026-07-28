package com.peakda.server.domain.search.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotFavoriteCount
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.data.domain.PageRequest as SpringPageRequest

class SearchServiceTest {

    private val spotRepository = mock(SpotRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val spotFavoriteRepository = mock(SpotFavoriteRepository::class.java)
    private val objectKeyUrlResolver = mock(ObjectKeyUrlResolver::class.java)

    private val service = SearchService(spotRepository, userRepository, spotFavoriteRepository, objectKeyUrlResolver)

    @Test
    fun `공백 검색어는 조회 없이 빈 페이지를 반환한다`() {
        val response = service.searchSpots("  ", PageRequest(page = 0, size = 20))

        assertThat(response.content).isEmpty()
        assertThat(response.totalElements).isEqualTo(0)
    }

    @Test
    fun `스팟 검색은 이름 부분일치로 조회해 매핑한다`() {
        val spot = spot(100L, "남산타워")
        val pageable = SpringPageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"))
        `when`(spotRepository.findByVisibleTrueAndNameContainingIgnoreCase("남산", pageable))
            .thenReturn(PageImpl(listOf(spot), pageable, 1))

        val response = service.searchSpots("남산", PageRequest(page = 0, size = 20))

        assertThat(response.content).hasSize(1)
        assertThat(response.content.first().spotId).isEqualTo(100L)
        assertThat(response.content.first().name).isEqualTo("남산타워")
        assertThat(response.totalElements).isEqualTo(1)
    }

    @Test
    fun `사용자 검색은 활성 사용자만 닉네임 부분일치로 조회한다`() {
        val user = user(7L, "피크다")
        val pageable = SpringPageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "nickname"))
        `when`(userRepository.findByStatusAndNicknameContainingIgnoreCase(UserStatus.ACTIVE, "피크", pageable))
            .thenReturn(PageImpl(listOf(user), pageable, 1))
        `when`(objectKeyUrlResolver.resolve(null)).thenReturn(null)

        val response = service.searchUsers("피크", PageRequest(page = 0, size = 20))

        assertThat(response.content).hasSize(1)
        assertThat(response.content.first().userId).isEqualTo(7L)
        assertThat(response.content.first().nickname).isEqualTo("피크다")
    }

    @Test
    fun `트렌딩은 찜 수 내림차순을 유지하고 존재하지 않는 스팟은 제외한다`() {
        `when`(spotFavoriteRepository.findTrendingSpotIds(SpringPageRequest.of(0, 10))).thenReturn(
            listOf(
                favoriteCount(100L, 5L),
                favoriteCount(999L, 3L),
                favoriteCount(200L, 2L),
            ),
        )
        `when`(spotRepository.findAllById(listOf(100L, 999L, 200L)))
            .thenReturn(listOf(spot(100L, "남산타워"), spot(200L, "여좌천")))

        val response = service.trending()

        assertThat(response.items).extracting<Long> { it.spotId }.containsExactly(100L, 200L)
        assertThat(response.items.first().favoriteCount).isEqualTo(5L)
    }

    private fun spot(id: Long, name: String): Spot {
        val spot = Spot(type = SpotType.ATTRACTION, name = name, latitude = 37.5, longitude = 127.0)
        ReflectionTestUtils.setField(spot, "id", id)
        return spot
    }

    private fun user(id: Long, nickname: String): User {
        val user = User(
            provider = OAuth2LoginType.KAKAO,
            providerId = "p-$id",
            nickname = nickname,
        )
        ReflectionTestUtils.setField(user, "id", id)
        return user
    }

    private fun favoriteCount(spotId: Long, count: Long) = object : SpotFavoriteCount {
        override val spotId: Long = spotId
        override val favoriteCount: Long = count
    }
}
