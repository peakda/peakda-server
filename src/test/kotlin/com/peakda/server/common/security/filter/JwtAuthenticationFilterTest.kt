package com.peakda.server.common.security.filter

import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.jwt.JwtClaims
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.session.application.AuthSessionCache
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional
import java.util.UUID

class JwtAuthenticationFilterTest {

    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val authSessionCache = mock(AuthSessionCache::class.java)
    private val filter = JwtAuthenticationFilter(
        jwtTokenProvider = jwtTokenProvider,
        cookieProperties = CookieProperties(domain = null, secure = false, sameSite = "Lax"),
        userRepository = userRepository,
        signupSessionService = mock(SignupSessionService::class.java),
        authSessionCache = authSessionCache,
    )
    private val user = User(provider = OAuth2LoginType.KAKAO, providerId = "provider-1", nickname = "사용자")

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `액세스 토큰의 세션 id 를 principal 에 담는다`() {
        givenToken(JwtClaims(USER_ID, SESSION_ID, refresh = false))

        doFilter()

        val principal = SecurityContextHolder.getContext().authentication?.principal as PrincipalDetails
        assertThat(principal.sessionId).isEqualTo(SESSION_ID)
    }

    @Test
    fun `해제된 세션의 액세스 토큰은 만료 전이라도 인증하지 않는다`() {
        givenToken(JwtClaims(USER_ID, SESSION_ID, refresh = false))
        `when`(authSessionCache.isRevoked(SESSION_ID)).thenReturn(true)

        doFilter()

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `리프레시 토큰은 액세스 토큰으로 쓸 수 없다`() {
        givenToken(JwtClaims(USER_ID, SESSION_ID, refresh = true))

        doFilter()

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `해제 여부를 확인하지 못하면 인증을 막지 않는다`() {
        givenToken(JwtClaims(USER_ID, SESSION_ID, refresh = false))
        `when`(authSessionCache.isRevoked(SESSION_ID)).thenThrow(IllegalStateException("redis down"))

        doFilter()

        assertThat(SecurityContextHolder.getContext().authentication).isNotNull
    }

    private fun givenToken(claims: JwtClaims) {
        `when`(jwtTokenProvider.parse(TOKEN)).thenReturn(claims)
        `when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))
    }

    private fun doFilter() {
        val request = MockHttpServletRequest("GET", "/api/users/me").apply {
            addHeader("Authorization", "Bearer $TOKEN")
        }
        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())
    }

    companion object {
        private const val TOKEN = "token"
        private const val USER_ID = 42L
        private val SESSION_ID = UUID.fromString("3f2b8c1e-6a7d-4f0e-9c2a-1b5d8e7f9a10")
    }
}
