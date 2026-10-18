package com.peakda.server.domain.auth.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.security.jwt.JwtClaims
import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.JwtTokenGenerator
import com.peakda.server.common.security.jwt.JwtTokenProvider
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.auth.session.application.AuthSessionCache
import com.peakda.server.domain.auth.session.application.AuthSessionProperties
import com.peakda.server.domain.auth.session.application.AuthSessionService
import com.peakda.server.domain.auth.session.application.RefreshTokenHash
import com.peakda.server.domain.auth.session.application.SessionDevice
import com.peakda.server.domain.auth.session.entity.AuthSession
import com.peakda.server.domain.auth.session.entity.SessionClientType
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 토큰 쌍 발급과 회전. 웹(쿠키)·앱(Bearer) 진입점이 모두 여기를 거치므로
 * 세션 생성과 탈취 감지 규칙이 한 곳에만 존재한다.
 *
 * 토큰은 로그인 세션(기기) 단위로 발급되고, 회전·폐기도 세션 단위로 일어난다.
 * 한 기기의 재발급 실패나 탈취 의심이 다른 기기의 로그인을 끊지 않는다.
 */
@Service
class TokenIssueService(
    private val jwtTokenGenerator: JwtTokenGenerator,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties,
    private val authSessionService: AuthSessionService,
    private val authSessionCache: AuthSessionCache,
    private val authSessionProperties: AuthSessionProperties,
    private val userRepository: UserRepository,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    /** 새 세션을 만들고 토큰 쌍을 발급한다. */
    @Transactional
    fun issue(user: User, device: SessionDevice): TokenResponse {
        val userId = requireNotNull(user.id) { "user id must exist" }
        val now = Instant.now()
        val sessionId = UUID.randomUUID()
        val tokens = generate(user, sessionId, device.clientType)
        authSessionService.open(
            userId = userId,
            sessionId = sessionId,
            device = device,
            refreshTokenHash = RefreshTokenHash.of(tokens.refreshToken),
            now = now,
            expiresAt = now.plusSeconds(tokens.refreshTokenExpiresIn),
        )
        return tokens
    }

    /**
     * 리프레시 토큰을 검증하고 새 토큰 쌍으로 회전한다.
     * - 현재 토큰이면 회전한다.
     * - 직전 토큰이고 회전 직후면(동시 재발급·응답 유실) 직전 회전 결과를 그대로 돌려준다.
     * - 그 밖의 옛 토큰이면 탈취로 보고 그 세션만 폐기한다.
     *
     * 세션 폐기 뒤 예외를 던지므로 예외로 롤백되지 않게 한다.
     *
     * @param device 세션 도입 전 토큰을 세션으로 옮길 때만 쓴다.
     */
    @Transactional(noRollbackFor = [AuthorizationException::class])
    fun rotate(refreshToken: String, device: SessionDevice): TokenResponse {
        val claims = jwtTokenProvider.parse(refreshToken)
            ?: throw AuthorizationException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        val sessionId = claims.sessionId ?: return migrateLegacy(claims, refreshToken, device)
        if (!claims.refresh) throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)

        val session = authSessionService.findForRotation(sessionId)
            ?.takeIf { it.userId == claims.userId }
            ?: throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)
        val now = Instant.now()
        if (session.isExpired(now)) {
            authSessionService.revoke(session)
            throw AuthorizationException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        val hash = RefreshTokenHash.of(refreshToken)
        return when {
            session.isCurrent(hash) -> rotateCurrent(session, hash, now)
            session.isRecentlyRotated(hash, now, authSessionProperties.rotationGrace) ->
                authSessionCache.findRotation(sessionId, hash)
                    ?: throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)
            else -> {
                authSessionService.revoke(session)
                log.warn("Refresh token 재사용 감지 - 탈취 의심, 세션 폐기. userId: {}, sessionId: {}", session.userId, sessionId)
                throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)
            }
        }
    }

    private fun rotateCurrent(session: AuthSession, hash: String, now: Instant): TokenResponse {
        val user = findUser(session.userId)
        val tokens = generate(user, session.id, session.clientType)
        session.rotate(
            newRefreshTokenHash = RefreshTokenHash.of(tokens.refreshToken),
            now = now,
            expiresAt = now.plusSeconds(tokens.refreshTokenExpiresIn),
        )
        authSessionCache.saveRotation(session.id, hash, tokens, authSessionProperties.rotationGrace)
        return tokens
    }

    /**
     * 세션 도입 전에 발급된 리프레시 토큰. 그때 저장해 둔 값과 같으면 새 세션으로 옮겨 로그인을 이어 준다.
     * 다르면 거절만 하고 저장값은 지우지 않는다 — 지우면 그 값을 가진 정상 기기까지 로그아웃된다.
     */
    private fun migrateLegacy(claims: JwtClaims, refreshToken: String, device: SessionDevice): TokenResponse {
        if (authSessionCache.findLegacyRefreshToken(claims.userId) != refreshToken) {
            throw AuthorizationException(ErrorCode.REFRESH_TOKEN_INVALID)
        }
        authSessionCache.deleteLegacyRefreshToken(claims.userId)
        return issue(findUser(claims.userId), device)
    }

    private fun findUser(userId: Long): User = userRepository.findById(userId)
        .orElseThrow { AuthorizationException(ErrorCode.RESOURCE_NOT_FOUND) }

    private fun generate(user: User, sessionId: UUID, clientType: SessionClientType): TokenResponse =
        jwtTokenGenerator.generateToken(
            userId = requireNotNull(user.id) { "user id must exist" },
            email = user.email,
            authorities = listOf("${PrincipalDetails.ROLE_PREFIX}${user.role.name}"),
            sessionId = sessionId,
            refreshTokenValidityInSeconds = when (clientType) {
                SessionClientType.APP -> jwtProperties.appRefreshTokenValidityInSeconds
                SessionClientType.WEB -> jwtProperties.refreshTokenValidityInSeconds
            },
        )
}
