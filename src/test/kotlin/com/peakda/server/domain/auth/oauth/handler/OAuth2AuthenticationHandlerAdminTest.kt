package com.peakda.server.domain.auth.oauth.handler

import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.common.security.principal.OAuth2SignupPrincipal
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.auth.app.application.AppLoginProperties
import com.peakda.server.domain.auth.app.application.AuthorizationCodeService
import com.peakda.server.domain.auth.application.TokenIssueService
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.session.application.SessionDevice
import com.peakda.server.domain.auth.session.application.WebSessionDevices
import com.peakda.server.domain.auth.signup.entity.SignupSession
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.servlet.HandlerExceptionResolver
import java.time.Instant

/** 백오피스에서 시작한 소셜 로그인의 성공·실패가 사용자용 웹이 아니라 백오피스로 돌아가는지 확인한다. */
class OAuth2AuthenticationHandlerAdminTest {

    private val cookieProperties = CookieProperties(domain = null, secure = true, sameSite = "Lax")
    private val tokenIssueService = mock(TokenIssueService::class.java)
    private val exceptionResolver = mock(HandlerExceptionResolver::class.java)
    private val failureHandler = OAuth2AuthenticationFailureHandler(exceptionResolver, AppLoginProperties())
    private val successHandler = OAuth2AuthenticationSuccessHandler(
        cookieProperties = cookieProperties,
        jwtProperties = JwtProperties(
            secret = "test-secret",
            accessTokenValidityInSeconds = 1_800,
            refreshTokenValidityInSeconds = 1_209_600,
        ),
        tokenIssueService = tokenIssueService,
        webSessionDevices = WebSessionDevices(cookieProperties),
        authorizationCodeService = mock(AuthorizationCodeService::class.java),
        appLoginProperties = AppLoginProperties(),
        oAuth2AuthenticationFailureHandler = failureHandler,
    ).also { ReflectionTestUtils.setField(it, "redirectUri", WEB_REDIRECT_URI) }

    @Test
    fun `백오피스 로그인에 성공하면 웹과 같은 쿠키를 받고 백오피스 첫 화면으로 돌아간다`() {
        val user = user()
        `when`(tokenIssueService.issue(sameUser(user), anyDevice())).thenReturn(TOKENS)
        val response = MockHttpServletResponse()

        successHandler.onAuthenticationSuccess(callback(ADMIN_STATE), response, authentication(PrincipalDetails(user)))

        assertThat(response.redirectedUrl).isEqualTo("/admin/index.html")
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
            .anyMatch { it.startsWith("access-token=access") }
            .anyMatch { it.startsWith("refresh-token=refresh") }
    }

    @Test
    fun `웹 로그인은 지금처럼 사용자용 웹으로 돌아간다`() {
        val user = user()
        `when`(tokenIssueService.issue(sameUser(user), anyDevice())).thenReturn(TOKENS)
        val response = MockHttpServletResponse()

        successHandler.onAuthenticationSuccess(callback(WEB_STATE), response, authentication(PrincipalDetails(user)))

        assertThat(response.redirectedUrl).isEqualTo(WEB_REDIRECT_URI)
    }

    @Test
    fun `가입되지 않은 계정은 가입 쿠키 없이 백오피스로 돌아가 사유를 받는다`() {
        val response = MockHttpServletResponse()

        successHandler.onAuthenticationSuccess(
            callback(ADMIN_STATE),
            response,
            authentication(OAuth2SignupPrincipal(signupSession())),
        )

        assertThat(response.redirectedUrl).isEqualTo("/admin/index.html?error=SIGNUP_REQUIRED")
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).noneMatch { it.startsWith("signup-token=") }
    }

    @Test
    fun `이용할 수 없는 계정은 토큰 없이 백오피스로 돌아가 사유를 받는다`() {
        val user = user().apply { status = UserStatus.SUSPENDED }
        val response = MockHttpServletResponse()

        successHandler.onAuthenticationSuccess(callback(ADMIN_STATE), response, authentication(PrincipalDetails(user)))

        assertThat(response.redirectedUrl).isEqualTo("/admin/index.html?error=USER_DISABLED")
        verifyNoInteractions(tokenIssueService)
    }

    @Test
    fun `백오피스 로그인 실패는 JSON 에러 대신 백오피스로 돌아간다`() {
        val request = callback(ADMIN_STATE)
        val response = MockHttpServletResponse()

        failureHandler.onAuthenticationFailure(request, response, BadCredentialsException("denied"))

        assertThat(response.redirectedUrl).isEqualTo("/admin/index.html?error=LOGIN_FAILED")
        verify(exceptionResolver, never()).resolveException(eq(request), eq(response), eq(null), anyException())
    }

    @Test
    fun `웹 로그인 실패는 지금처럼 공통 예외 처리로 넘긴다`() {
        val request = callback(WEB_STATE)
        val response = MockHttpServletResponse()
        val exception = BadCredentialsException("denied")

        failureHandler.onAuthenticationFailure(request, response, exception)

        assertThat(response.redirectedUrl).isNull()
        verify(exceptionResolver).resolveException(request, response, null, exception)
    }

    private fun callback(state: String) = MockHttpServletRequest("GET", "/login/oauth2/code/kakao").apply {
        addParameter("state", state)
    }

    private fun authentication(principal: Any) = TestingAuthenticationToken(principal, null)

    private fun user(): User = User(
        provider = OAuth2LoginType.KAKAO,
        providerId = "provider-1",
        nickname = "운영자",
        email = "admin@example.com",
    ).apply { role = UserRole.ADMIN }.also { ReflectionTestUtils.setField(it, "id", 1L) }

    private fun signupSession() = SignupSession(
        token = "signup-token-value",
        provider = OAuth2LoginType.KAKAO,
        providerId = "provider-2",
        expiresAt = Instant.parse("2026-11-22T00:15:00Z"),
    ).also { ReflectionTestUtils.setField(it, "id", 10L) }

    /** Mockito.eq() 도 null 을 돌려주므로 같은 방식으로 감싼다. */
    private fun sameUser(user: User): User = eq(user) ?: user

    /** Mockito.any() 의 null 반환을 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun anyDevice(): SessionDevice = any(SessionDevice::class.java) ?: SessionDevice.unknownApp()

    private fun anyException(): Exception = any(Exception::class.java) ?: IllegalStateException()

    companion object {
        private const val WEB_REDIRECT_URI = "https://www.peakda.com/auth/callback"
        private const val WEB_STATE = "random-state"
        private const val ADMIN_STATE = "random-state.admin"
        private val TOKENS = TokenResponse(
            tokenType = "Bearer",
            accessToken = "access",
            refreshToken = "refresh",
            refreshTokenExpiresIn = 1_209_600,
        )
    }
}
