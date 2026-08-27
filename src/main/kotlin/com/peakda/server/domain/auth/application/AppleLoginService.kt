package com.peakda.server.domain.auth.application

import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.JwtTokenGenerator
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.auth.oauth.apple.AppleIdTokenVerifier
import com.peakda.server.domain.auth.oauth.model.AppleOAuth2UserInfo
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.presentation.response.AppleLoginResponse
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Apple 네이티브 로그인 처리.
 *
 * iOS 가 보낸 id_token 을 검증한 뒤 기존 회원이면 로그인 토큰을, 신규면 가입 세션 토큰을 쿠키로 발급한다.
 * 쿠키 세팅 방식은 웹 리다이렉트 플로우의 OAuth2AuthenticationSuccessHandler 와 동일하다.
 */
@Service
class AppleLoginService(
    private val appleIdTokenVerifier: AppleIdTokenVerifier,
    private val userRepository: UserRepository,
    private val signupSessionService: SignupSessionService,
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val refreshTokenService: RefreshTokenService,
    private val cookieProperties: CookieProperties,
    private val jwtProperties: JwtProperties,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun login(identityToken: String, response: HttpServletResponse): AppleLoginResponse {
        val claims = appleIdTokenVerifier.verify(identityToken)
        val userInfo = AppleOAuth2UserInfo(sub = claims.sub, email = claims.email)

        val user = userRepository.findByProviderAndProviderId(OAuth2LoginType.APPLE, userInfo.getProviderId())
        return if (user != null) {
            issueLoginCookies(user, response)
            AppleLoginResponse(signupRequired = false)
        } else {
            issueSignupCookie(userInfo, response)
            AppleLoginResponse(signupRequired = true)
        }
    }

    private fun issueLoginCookies(user: User, response: HttpServletResponse) {
        val userId = requireNotNull(user.id)
        log.info("Apple 로그인 성공. userId={}", userId)

        val tokenResponse = jwtTokenGenerator.generateToken(
            userId = userId,
            email = user.email,
            authorities = listOf("${PrincipalDetails.ROLE_PREFIX}${user.role.name}"),
        )
        refreshTokenService.saveRefreshToken(userId, tokenResponse.refreshToken)

        response.addCookie(
            CookieUtils.createAccessTokenCookie(
                token = tokenResponse.accessToken,
                maxAge = jwtProperties.accessTokenValidityInSeconds,
                properties = cookieProperties,
            ),
        )
        response.addCookie(
            CookieUtils.createRefreshTokenCookie(
                token = tokenResponse.refreshToken,
                maxAge = jwtProperties.refreshTokenValidityInSeconds,
                properties = cookieProperties,
            ),
        )
        response.addCookie(CookieUtils.deleteSignupTokenCookie(cookieProperties))
    }

    private fun issueSignupCookie(userInfo: AppleOAuth2UserInfo, response: HttpServletResponse) {
        val signupSession = signupSessionService.createOrRefresh(OAuth2LoginType.APPLE, userInfo)
        log.info("Apple 회원가입 필요. signupSessionId={}", signupSession.id)

        response.addCookie(CookieUtils.createSignupTokenCookie(signupSession.token, cookieProperties))
        response.addCookie(CookieUtils.deleteAccessTokenCookie(cookieProperties))
        response.addCookie(CookieUtils.deleteRefreshTokenCookie(cookieProperties))
    }

    private fun HttpServletResponse.addCookie(cookie: ResponseCookie) {
        addHeader("Set-Cookie", cookie.toString())
    }
}
