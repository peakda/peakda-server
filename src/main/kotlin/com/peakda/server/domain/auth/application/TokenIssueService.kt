package com.peakda.server.domain.auth.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.security.jwt.JwtTokenGenerator
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 토큰 쌍 발급과 회전. 웹(쿠키)·앱(Bearer) 진입점이 모두 여기를 거치므로
 * 리프레시 토큰 저장과 탈취 감지 규칙이 한 곳에만 존재한다.
 */
@Service
class TokenIssueService(
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenService: RefreshTokenService,
    private val userRepository: UserRepository,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /** 새 토큰 쌍을 발급하고 리프레시 토큰을 Redis 에 저장한다. */
    fun issue(user: User): TokenResponse {
        val userId = requireNotNull(user.id) { "user id must exist" }
        val tokenResponse = jwtTokenGenerator.generateToken(
            userId = userId,
            email = user.email,
            authorities = listOf("${PrincipalDetails.ROLE_PREFIX}${user.role.name}"),
        )
        refreshTokenService.saveRefreshToken(userId, tokenResponse.refreshToken)
        return tokenResponse
    }

    /**
     * 리프레시 토큰을 검증하고 새 토큰 쌍으로 회전한다.
     * Redis 저장본과 다르면 탈취로 보고 해당 사용자의 리프레시 토큰을 전부 폐기한다.
     */
    fun rotate(refreshToken: String): TokenResponse {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw AuthorizationException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }
        val userId = jwtTokenProvider.getUserIdFromToken(refreshToken)
            ?: throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)

        val storedToken = refreshTokenService.getRefreshToken(userId)
        if (storedToken == null || storedToken != refreshToken) {
            refreshTokenService.deleteRefreshToken(userId)
            log.warn("Refresh token 불일치 감지 - 탈취 의심. userId: {}", userId)
            throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { AuthorizationException(ErrorCode.RESOURCE_NOT_FOUND) }
        return issue(user)
    }
}
