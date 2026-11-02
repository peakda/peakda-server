package com.peakda.server.domain.auth.apple.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.domain.auth.app.application.AppAuthService
import com.peakda.server.domain.auth.app.presentation.response.AppAuthStatus
import com.peakda.server.domain.auth.app.presentation.response.AppTokenResponse
import com.peakda.server.domain.auth.application.TokenIssueService
import com.peakda.server.domain.auth.oauth.model.AppleOAuth2UserInfo
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.oauth.model.OAuth2UserInfo
import com.peakda.server.domain.auth.session.application.SessionDevice
import com.peakda.server.domain.auth.session.entity.SessionPlatform
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.auth.signup.entity.SignupSession
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.time.Instant

class AppleLoginServiceTest {

    private val appleTokenService = mock(AppleTokenService::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val signupSessionService = mock(SignupSessionService::class.java)
    private val tokenIssueService = mock(TokenIssueService::class.java)
    private val appAuthService = mock(AppAuthService::class.java)
    private var decodeCount = 0

    // 서명 검증은 AppleIdentityTokenVerifierTest 가 맡는다. 여기서는 정해진 토큰만 통과시키는 디코더를 쓴다.
    private val decoder = JwtDecoder { token ->
        decodeCount++
        if (token != VALID_TOKEN) throw BadJwtException("invalid")
        Jwt.withTokenValue(token)
            .header("alg", "RS256")
            .subject(APPLE_USER_ID)
            .claim("email", EMAIL)
            .claim("nonce", AppleIdentityTokenVerifier.sha256Hex(NONCE))
            .build()
    }

    private fun service(enabled: Boolean = true) = AppleLoginService(
        properties = AppleProperties(enabled = enabled, teamId = "TEAM", keyId = "KEY", privateKeyBase64 = "unused"),
        identityTokenVerifier = AppleIdentityTokenVerifier(decoder),
        appleTokenService = appleTokenService,
        userRepository = userRepository,
        signupSessionService = signupSessionService,
        tokenIssueService = tokenIssueService,
        appAuthService = appAuthService,
    )

    @Test
    fun `가입한 애플 회원은 앱 토큰을 받고 애플 리프레시 토큰을 저장한다`() {
        val user = userWithStatus(UserStatus.ACTIVE)
        `when`(userRepository.findByProviderAndProviderId(OAuth2LoginType.APPLE, APPLE_USER_ID)).thenReturn(user)
        `when`(tokenIssueService.issue(user, DEVICE)).thenReturn(TOKENS)
        `when`(appAuthService.tokens(TOKENS)).thenReturn(AUTHENTICATED)

        val response = service().login(VALID_TOKEN, CODE, NONCE, DEVICE)

        assertThat(response).isEqualTo(AUTHENTICATED)
        verify(appleTokenService).store(APPLE_USER_ID, CODE)
    }

    @Test
    fun `처음 로그인한 사용자는 애플 정보로 가입 세션을 받는다`() {
        val session = mock(SignupSession::class.java)
        `when`(session.token).thenReturn(SIGNUP_TOKEN)
        `when`(session.expiresAt).thenReturn(Instant.now().plusSeconds(900))
        `when`(signupSessionService.createOrRefresh(apple(), anyUserInfo())).thenReturn(session)

        val response = service().login(VALID_TOKEN, CODE, NONCE, DEVICE)

        assertThat(response.status).isEqualTo(AppAuthStatus.SIGNUP_REQUIRED)
        assertThat(response.signupToken).isEqualTo(SIGNUP_TOKEN)
        assertThat(response.signupTokenExpiresIn).isBetween(890L, 900L)
        val userInfo = ArgumentCaptor.forClass(OAuth2UserInfo::class.java)
        verify(signupSessionService).createOrRefresh(apple(), userInfo.capture() ?: PLACEHOLDER_INFO)
        assertThat(userInfo.value.getProviderId()).isEqualTo(APPLE_USER_ID)
        assertThat(userInfo.value.getEmail()).isEqualTo(EMAIL)
        verify(appleTokenService).store(APPLE_USER_ID, CODE)
    }

    @Test
    fun `정지된 회원은 로그인할 수 없다`() {
        val user = userWithStatus(UserStatus.SUSPENDED)
        `when`(userRepository.findByProviderAndProviderId(OAuth2LoginType.APPLE, APPLE_USER_ID)).thenReturn(user)

        assertThatThrownBy { service().login(VALID_TOKEN, CODE, NONCE, DEVICE) }
            .isInstanceOf(AuthorizationException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.UNAUTHORIZED)
        verifyNoInteractions(tokenIssueService)
    }

    @Test
    fun `검증에 실패한 토큰은 애플 API 를 부르거나 회원을 찾기 전에 거부한다`() {
        assertThatThrownBy { service().login("forged-token", CODE, NONCE, DEVICE) }
            .isInstanceOf(AuthorizationException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.APPLE_IDENTITY_TOKEN_INVALID)
        verifyNoInteractions(appleTokenService, userRepository, signupSessionService)
    }

    @Test
    fun `서버에서 애플 로그인을 끄면 토큰을 보지도 않고 거부한다`() {
        assertThatThrownBy { service(enabled = false).login(VALID_TOKEN, CODE, NONCE, DEVICE) }
            .isInstanceOf(AuthorizationException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED)
        assertThat(decodeCount).isZero()
        verifyNoInteractions(appleTokenService, userRepository)
    }

    private fun userWithStatus(status: UserStatus): User = mock(User::class.java).also { `when`(it.status).thenReturn(status) }

    private fun apple(): OAuth2LoginType = ArgumentMatchers.eq(OAuth2LoginType.APPLE) ?: OAuth2LoginType.APPLE

    private fun anyUserInfo(): OAuth2UserInfo = ArgumentMatchers.any(OAuth2UserInfo::class.java) ?: PLACEHOLDER_INFO

    companion object {
        private const val VALID_TOKEN = "apple-identity-token"
        private const val CODE = "apple-authorization-code"
        private const val NONCE = "raw-nonce"
        private const val SIGNUP_TOKEN = "signup-token"
        private const val APPLE_USER_ID = "001234.0123456789abcdef0123456789abcdef.0123"
        private const val EMAIL = "abc123@privaterelay.appleid.com"
        private val DEVICE = SessionDevice.app("device-1", "iPhone17,1", SessionPlatform.IOS)
        private val TOKENS = TokenResponse(
            tokenType = "Bearer",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            refreshTokenExpiresIn = 2_592_000,
        )
        private val AUTHENTICATED = AppTokenResponse.authenticated(TOKENS, 1800, 2_592_000)
        private val PLACEHOLDER_INFO = AppleOAuth2UserInfo(mapOf("sub" to "placeholder"))
    }
}
