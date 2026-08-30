package com.peakda.server.domain.auth.app.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.domain.auth.app.presentation.response.AppAuthStatus
import com.peakda.server.domain.auth.application.TokenIssueService
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.auth.signup.entity.SignupSession
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional

class AppAuthServiceTest {

    private val authorizationCodeService = mock(AuthorizationCodeService::class.java)
    private val tokenIssueService = mock(TokenIssueService::class.java)
    private val signupSessionService = mock(SignupSessionService::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val jwtProperties = JwtProperties(
        secret = "secret",
        accessTokenValidityInSeconds = 1800,
        refreshTokenValidityInSeconds = 604800,
    )
    private val service = AppAuthService(
        authorizationCodeService,
        tokenIssueService,
        signupSessionService,
        userRepository,
        jwtProperties,
    )

    @Test
    fun `가입이 끝난 사용자의 코드는 액세스 토큰과 리프레시 토큰으로 바뀐다`() {
        val user = activeUser()
        `when`(authorizationCodeService.consume(CODE)).thenReturn(AuthorizationCodePayload.Authenticated(USER_ID))
        `when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))
        `when`(tokenIssueService.issue(user)).thenReturn(TOKENS)

        val response = service.exchange(CODE)

        assertThat(response.status).isEqualTo(AppAuthStatus.AUTHENTICATED)
        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo("refresh-token")
        assertThat(response.tokenType).isEqualTo("Bearer")
        assertThat(response.accessTokenExpiresIn).isEqualTo(1800)
        assertThat(response.refreshTokenExpiresIn).isEqualTo(604800)
        assertThat(response.signupToken).isNull()
    }

    @Test
    fun `가입이 남은 사용자의 코드는 가입 세션 토큰으로 바뀐다`() {
        val session = mock(SignupSession::class.java)
        `when`(session.expiresAt).thenReturn(Instant.now().plusSeconds(900))
        `when`(authorizationCodeService.consume(CODE))
            .thenReturn(AuthorizationCodePayload.SignupRequired(SIGNUP_TOKEN))
        `when`(signupSessionService.findValidByToken(SIGNUP_TOKEN)).thenReturn(session)

        val response = service.exchange(CODE)

        assertThat(response.status).isEqualTo(AppAuthStatus.SIGNUP_REQUIRED)
        assertThat(response.signupToken).isEqualTo(SIGNUP_TOKEN)
        assertThat(response.signupTokenExpiresIn).isBetween(890, 900)
        assertThat(response.accessToken).isNull()
    }

    @Test
    fun `이미 쓰였거나 만료된 코드는 교환하지 못한다`() {
        `when`(authorizationCodeService.consume(CODE)).thenReturn(null)

        assertThatThrownBy { service.exchange(CODE) }
            .isInstanceOf(AuthorizationException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_CODE_INVALID)
    }

    @Test
    fun `코드 발급 뒤 계정이 잠기면 교환 시점에 막는다`() {
        val user = mock(User::class.java)
        `when`(user.status).thenReturn(UserStatus.SUSPENDED)
        `when`(authorizationCodeService.consume(CODE)).thenReturn(AuthorizationCodePayload.Authenticated(USER_ID))
        `when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))

        assertThatThrownBy { service.exchange(CODE) }
            .isInstanceOf(AuthorizationException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED)
    }

    @Test
    fun `만료된 가입 세션을 가리키는 코드는 교환하지 못한다`() {
        `when`(authorizationCodeService.consume(CODE))
            .thenReturn(AuthorizationCodePayload.SignupRequired(SIGNUP_TOKEN))
        `when`(signupSessionService.findValidByToken(SIGNUP_TOKEN)).thenReturn(null)

        assertThatThrownBy { service.exchange(CODE) }
            .isInstanceOf(AuthorizationException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SIGNUP_SESSION_EXPIRED)
    }

    @Test
    fun `재발급은 회전 결과를 그대로 옮긴다`() {
        `when`(tokenIssueService.rotate("old-refresh-token")).thenReturn(TOKENS)

        val response = service.refresh("old-refresh-token")

        assertThat(response.status).isEqualTo(AppAuthStatus.AUTHENTICATED)
        assertThat(response.accessToken).isEqualTo("access-token")
        assertThat(response.refreshToken).isEqualTo("refresh-token")
    }

    private fun activeUser(): User = mock(User::class.java).also { `when`(it.status).thenReturn(UserStatus.ACTIVE) }

    companion object {
        private const val CODE = "one-time-code"
        private const val USER_ID = 42L
        private const val SIGNUP_TOKEN = "signup-token"
        private val TOKENS = TokenResponse(
            tokenType = "Bearer",
            accessToken = "access-token",
            refreshToken = "refresh-token",
        )
    }
}
