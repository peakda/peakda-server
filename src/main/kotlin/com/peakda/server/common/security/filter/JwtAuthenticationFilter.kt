package com.peakda.server.common.security.filter

import com.peakda.server.domain.auth.session.application.AuthSessionCache
import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.common.security.cookie.CookieProperties
import com.peakda.server.common.security.cookie.CookieUtils
import com.peakda.server.common.security.jwt.BearerTokens
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.common.security.principal.SignupSessionPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val cookieProperties: CookieProperties,
    private val userRepository: UserRepository,
    private val signupSessionService: SignupSessionService,
    private val authSessionCache: AuthSessionCache,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.requestURI == "/api/auth/signup/nickname/check") {
            authenticateUser(request)
            if (SecurityContextHolder.getContext().authentication == null) {
                authenticateSignupSession(request)
            }
        } else if (request.requestURI.startsWith("/api/auth/signup/")) {
            authenticateSignupSession(request)
        } else {
            authenticateUser(request)
        }
        filterChain.doFilter(request, response)
    }

    /**
     * 앱은 Bearer 헤더로, 웹은 쿠키로 인증한다. 둘 다 오면 헤더를 우선한다.
     * 리프레시 토큰은 액세스 토큰으로 받지 않고, 기기 목록에서 해제된 세션의 토큰은 만료 전이라도 막는다.
     */
    private fun authenticateUser(request: HttpServletRequest) {
        val token = BearerTokens.from(request)
            ?: CookieUtils.getAccessTokenFromCookies(request.cookies, cookieProperties)
            ?: return
        val claims = jwtTokenProvider.parse(token) ?: return
        if (claims.refresh) return
        if (claims.sessionId != null && isRevoked(claims.sessionId)) return
        val user = userRepository.findById(claims.userId).orElse(null) ?: return
        val principal = PrincipalDetails(user, sessionId = claims.sessionId)
        if (!principal.isEnabled) return
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }

    /** Redis 장애로 모든 요청이 막히지 않도록, 확인할 수 없으면 해제되지 않은 것으로 본다. */
    private fun isRevoked(sessionId: UUID): Boolean = runCatching { authSessionCache.isRevoked(sessionId) }
        .onFailure { log.warn("세션 해제 여부 확인 실패. sessionId={}", sessionId, it) }
        .getOrDefault(false)

    private fun authenticateSignupSession(request: HttpServletRequest) {
        val token = BearerTokens.from(request)
            ?: CookieUtils.getSignupTokenFromCookies(request.cookies, cookieProperties)
            ?: return
        val signupSession = signupSessionService.findValidByToken(token) ?: return
        val principal = SignupSessionPrincipal(signupSession)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }
}
