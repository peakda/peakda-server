package com.peakda.server.global.security.filter

import com.peakda.server.domain.auth.signup.application.SignupSessionService
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.global.security.cookie.CookieProperties
import com.peakda.server.global.security.cookie.CookieUtils
import com.peakda.server.global.security.jwt.JwtTokenProvider
import com.peakda.server.global.security.principal.PrincipalDetails
import com.peakda.server.global.security.principal.SignupSessionPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val cookieProperties: CookieProperties,
    private val userRepository: UserRepository,
    private val signupSessionService: SignupSessionService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.requestURI.startsWith("/api/auth/signup/")) {
            authenticateSignupSession(request)
        } else {
            authenticateUser(request)
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticateUser(request: HttpServletRequest) {
        val token = CookieUtils.getAccessTokenFromCookies(request.cookies, cookieProperties)
        if (token != null && jwtTokenProvider.validateToken(token)) {
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            if (userId != null) {
                userRepository.findById(userId).ifPresent { user ->
                    val principal = PrincipalDetails(user)
                    val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }
    }

    private fun authenticateSignupSession(request: HttpServletRequest) {
        val token = CookieUtils.getSignupTokenFromCookies(request.cookies, cookieProperties) ?: return
        val signupSession = signupSessionService.findValidByToken(token) ?: return
        val principal = SignupSessionPrincipal(signupSession)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }
}
