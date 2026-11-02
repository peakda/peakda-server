package com.peakda.server.domain.auth.apple.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.domain.auth.app.application.AppAuthService
import com.peakda.server.domain.auth.app.presentation.response.AppTokenResponse
import com.peakda.server.domain.auth.application.TokenIssueService
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.session.application.SessionDevice
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * iOS 앱의 Sign in with Apple. 앱이 애플에서 받은 identity token 을 검증하고,
 * 소셜 로그인 코드 교환(AppAuthService.exchange)과 같은 응답을 돌려준다.
 *
 * 트랜잭션을 걸지 않는다. 애플 API 호출(코드 교환)이 DB 커넥션을 붙잡지 않도록
 * 저장·토큰 발급은 각 서비스·저장소의 트랜잭션에 맡긴다.
 */
@Service
class AppleLoginService(
    private val properties: AppleProperties,
    private val identityTokenVerifier: AppleIdentityTokenVerifier,
    private val appleTokenService: AppleTokenService,
    private val userRepository: UserRepository,
    private val signupSessionService: SignupSessionService,
    private val tokenIssueService: TokenIssueService,
    private val appAuthService: AppAuthService,
) {

    fun login(identityToken: String, authorizationCode: String, nonce: String, device: SessionDevice): AppTokenResponse {
        if (!properties.enabled) {
            throw AuthorizationException(ErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED)
        }

        val userInfo = identityTokenVerifier.verify(identityToken, nonce)
        val appleUserId = userInfo.getProviderId()
        appleTokenService.store(appleUserId, authorizationCode)

        val user = userRepository.findByProviderAndProviderId(OAuth2LoginType.APPLE, appleUserId)
        if (user != null) {
            if (user.status != UserStatus.ACTIVE) {
                throw AuthorizationException(ErrorCode.UNAUTHORIZED)
            }
            return appAuthService.tokens(tokenIssueService.issue(user, device))
        }

        val session = signupSessionService.createOrRefresh(OAuth2LoginType.APPLE, userInfo)
        return AppTokenResponse.signupRequired(
            signupToken = session.token,
            signupTokenExpiresIn = Duration.between(Instant.now(), session.expiresAt).seconds.coerceAtLeast(0),
        )
    }
}
