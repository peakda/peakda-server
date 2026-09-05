package com.peakda.server.domain.location.application

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.location.entity.LocationAccessChannel
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.entity.LocationUsageLog
import com.peakda.server.domain.location.repository.LocationUsageLogRepository
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant

class LocationUsageQueryServiceTest {

    private val locationUsageLogRepository = mock(LocationUsageLogRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val service = LocationUsageQueryService(locationUsageLogRepository, userRepository)
    private val pageable = PageRequest.of(0, 20)
    private val searchedUsersPageable = PageRequest.of(0, MAX_SEARCHED_USERS)

    @Test
    fun `검색어가 없으면 사용자 조건 없이 조회한다`() {
        `when`(locationUsageLogRepository.search(null, null, null, pageable))
            .thenReturn(PageImpl(emptyList<LocationUsageLog>(), pageable, 0))

        service.list(null, null, null, null, pageable)

        verify(locationUsageLogRepository).search(null, null, null, pageable)
        verifyNoMoreInteractions(locationUsageLogRepository)
    }

    @Test
    fun `이메일 검색어가 있으면 대상 사용자로 좁혀 조회한다`() {
        `when`(userRepository.findIdsByEmailPattern("%ex1@xxx.com%", searchedUsersPageable))
            .thenReturn(listOf(7L))
        `when`(
            locationUsageLogRepository.searchByUserIds(
                listOf(7L),
                LocationServiceType.BLOOM_MAP,
                null,
                null,
                pageable,
            ),
        ).thenReturn(PageImpl(emptyList<LocationUsageLog>(), pageable, 0))

        service.list("ex1@xxx.com", LocationServiceType.BLOOM_MAP, null, null, pageable)

        verify(locationUsageLogRepository)
            .searchByUserIds(listOf(7L), LocationServiceType.BLOOM_MAP, null, null, pageable)
        verifyNoMoreInteractions(locationUsageLogRepository)
    }

    @Test
    fun `검색어의 LIKE 와일드카드는 문자 그대로 비교한다`() {
        `when`(userRepository.findIdsByEmailPattern("%100\\%\\_test%", searchedUsersPageable))
            .thenReturn(emptyList())

        service.list("100%_test", null, null, null, pageable)

        verify(userRepository).findIdsByEmailPattern("%100\\%\\_test%", searchedUsersPageable)
    }

    @Test
    fun `검색어에 걸리는 사용자가 없으면 확인자료를 조회하지 않고 빈 페이지를 준다`() {
        `when`(userRepository.findIdsByEmailPattern("%없는주소@xxx.com%", searchedUsersPageable))
            .thenReturn(emptyList())

        val result = service.list("없는주소@xxx.com", null, null, null, pageable)

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isZero()
        verifyNoInteractions(locationUsageLogRepository)
    }

    @Test
    fun `확인자료에 대상 사용자의 이메일과 닉네임을 채워 준다`() {
        `when`(locationUsageLogRepository.search(null, null, null, pageable))
            .thenReturn(PageImpl(listOf(usageLog(userId = 7L)), pageable, 1))
        `when`(userRepository.findAllById(listOf(7L))).thenReturn(listOf(user(id = 7L, email = "ex1@xxx.com")))

        val result = service.list(null, null, null, null, pageable)

        assertThat(result.content).singleElement().satisfies({ response ->
            assertThat(response.userId).isEqualTo(7L)
            assertThat(response.email).isEqualTo("ex1@xxx.com")
            assertThat(response.nickname).isEqualTo("피크다")
            assertThat(response.channel).isEqualTo(LocationAccessChannel.ANDROID)
            assertThat(response.service).isEqualTo(LocationServiceType.BLOOM_MAP)
            assertThat(response.usedAt).isEqualTo(Instant.parse("2026-08-17T12:49:24Z"))
        })
    }

    @Test
    fun `계정이 남아 있지 않은 기록도 사용자 정보 없이 반환한다`() {
        `when`(locationUsageLogRepository.search(null, null, null, pageable))
            .thenReturn(PageImpl(listOf(usageLog(userId = 99L)), pageable, 1))
        `when`(userRepository.findAllById(listOf(99L))).thenReturn(emptyList())

        val result = service.list(null, null, null, null, pageable)

        assertThat(result.content).singleElement().satisfies({ response ->
            assertThat(response.userId).isEqualTo(99L)
            assertThat(response.email).isNull()
            assertThat(response.nickname).isNull()
        })
    }

    private fun usageLog(userId: Long): LocationUsageLog =
        LocationUsageLog(
            userId = userId,
            channel = LocationAccessChannel.ANDROID,
            service = LocationServiceType.BLOOM_MAP,
            usedAt = Instant.parse("2026-08-17T12:49:24Z"),
        ).also { ReflectionTestUtils.setField(it, "id", 1001L) }

    private fun user(id: Long, email: String): User =
        User(
            provider = OAuth2LoginType.KAKAO,
            providerId = "provider-$id",
            nickname = "피크다",
            email = email,
        ).also { ReflectionTestUtils.setField(it, "id", id) }

    companion object {
        /** LocationUsageQueryService 의 이메일 검색 대상자 상한과 같은 값 */
        private const val MAX_SEARCHED_USERS = 500
    }
}
