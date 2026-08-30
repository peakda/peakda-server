package com.peakda.server.domain.auth.app.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.domain.auth.app.presentation.response.AppTokenResponse
import com.peakda.server.domain.auth.application.TokenIssueService
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * 앱 로그인의 코드 교환·토큰 재발급.
 * 앱은 Custom Tab 에서 받은 쿠키를 쓸 수 없어, 딥링크로 받은 일회성 코드를 여기서 토큰으로 바꾼다.
 */
@Service
class AppAuthService(
    private val authorizationCodeService: AuthorizationCodeService,
    private val tokenIssueService: TokenIssueService,
    private val signupSessionService: SignupSessionService,
    private val userRepository: UserRepository,
    private val jwtProperties: JwtProperties,
) {

    fun exchange(code: String): AppTokenResponse =
        when (val payload = authorizationCodeService.consume(code)) {
            null -> throw AuthorizationException(ErrorCode.AUTH_CODE_INVALID)
            is AuthorizationCodePayload.Authenticated -> authenticated(payload.userId)
            is AuthorizationCodePayload.SignupRequired -> signupRequired(payload.signupToken)
        }

    fun refresh(refreshToken: String): AppTokenResponse = tokens(tokenIssueService.rotate(refreshToken))

    /** 다른 진입점(가입 완료)에서 발급한 토큰을 앱 응답 형식으로 옮긴다. */
    fun tokens(tokens: TokenResponse): AppTokenResponse = AppTokenResponse.authenticated(
        tokens = tokens,
        accessTokenExpiresIn = jwtProperties.accessTokenValidityInSeconds,
        refreshTokenExpiresIn = jwtProperties.refreshTokenValidityInSeconds,
    )

    /**
     * 코드 발급과 교환 사이(짧지만 0 은 아니다)에 계정 상태가 바뀔 수 있으므로 여기서 다시 확인한다.
     */
    private fun authenticated(userId: Long): AppTokenResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AuthorizationException(ErrorCode.USER_NOT_FOUND) }
        if (user.status != UserStatus.ACTIVE) {
            throw AuthorizationException(ErrorCode.UNAUTHORIZED)
        }
        return tokens(tokenIssueService.issue(user))
    }

    private fun signupRequired(signupToken: String): AppTokenResponse {
        val session = signupSessionService.findValidByToken(signupToken)
            ?: throw AuthorizationException(ErrorCode.SIGNUP_SESSION_EXPIRED)
        return AppTokenResponse.signupRequired(
            signupToken = signupToken,
            signupTokenExpiresIn = Duration.between(Instant.now(), session.expiresAt).seconds.coerceAtLeast(0),
        )
    }
}
