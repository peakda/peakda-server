package com.peakda.server.domain.auth.oauth.handler

import com.peakda.server.domain.auth.application.TokenIssueService
import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.principal.OAuth2SignupPrincipal
import com.peakda.server.common.security.principal.PrincipalDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2AuthenticationSuccessHandler(
    private val cookieProperties: CookieProperties,
    private val jwtProperties: JwtProperties,
    private val tokenIssueService: TokenIssueService,
    private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,
) : AuthenticationSuccessHandler {

    private val log = LoggerFactory.getLogger(this::class.java)

    @Value("\${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private lateinit var redirectUri: String

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal

        if (principal is OAuth2SignupPrincipal) {
            val signupSession = principal.getSignupSession()
            log.info("OAuth2 signup required. signupSessionId={}", signupSession.id)
            val signupTokenCookie = CookieUtils.createSignupTokenCookie(signupSession.token, cookieProperties)
            val accessTokenCookie = CookieUtils.deleteAccessTokenCookie(cookieProperties)
            val refreshTokenCookie = CookieUtils.deleteRefreshTokenCookie(cookieProperties)
            response.addHeader("Set-Cookie", accessTokenCookie.toString())
            response.addHeader("Set-Cookie", refreshTokenCookie.toString())
            response.addHeader("Set-Cookie", signupTokenCookie.toString())
            response.sendRedirect(redirectUri)
            return
        }

        principal as PrincipalDetails
        if (!principal.isEnabled) {
            SecurityContextHolder.clearContext()
            oAuth2AuthenticationFailureHandler.onAuthenticationFailure(
                request,
                response,
                DisabledException("로그인할 수 없는 사용자 상태입니다."),
            )
            return
        }
        val user = principal.getUser()
        val userId = requireNotNull(user.id)

        log.info("OAuth2 authentication successful. userId={}, status={}", userId, user.status)

        val tokenResponse = tokenIssueService.issue(user)

        val accessTokenCookie = CookieUtils.createAccessTokenCookie(
            token = tokenResponse.accessToken,
            maxAge = jwtProperties.accessTokenValidityInSeconds,
            properties = cookieProperties
        )
        val refreshTokenCookie = CookieUtils.createRefreshTokenCookie(
            token = tokenResponse.refreshToken,
            maxAge = jwtProperties.refreshTokenValidityInSeconds,
            properties = cookieProperties
        )
        val signupTokenCookie = CookieUtils.deleteSignupTokenCookie(cookieProperties)

        response.addHeader("Set-Cookie", accessTokenCookie.toString())
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())
        response.addHeader("Set-Cookie", signupTokenCookie.toString())

        response.sendRedirect(redirectUri)
    }
}
