package com.peakda.server.domain.auth.session.application

import com.peakda.server.common.security.jwt.JwtProperties
import com.peakda.server.common.security.jwt.TokenResponse
import com.peakda.server.domain.auth.application.RotatedTokens
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

/**
 * 세션 관련 Redis 상태.
 * - 해제된 세션 표시: 액세스 토큰은 만료 전까지 살아 있으므로, 해제 즉시 막기 위해 액세스 토큰 유효기간만큼 남긴다.
 * - 직전 회전 결과: 같은 리프레시 토큰으로 다시 온 재발급 요청에 같은 토큰 쌍을 돌려주기 위해 잠깐 둔다.
 * - 세션 도입 전 리프레시 토큰(`refresh:{userId}`): 배포 시점에 로그인해 있던 사용자를 세션으로 옮기는 데만 쓴다.
 */
@Component
class AuthSessionCache(
    private val redisTemplate: StringRedisTemplate,
    private val jwtProperties: JwtProperties,
) {

    fun markRevoked(sessionIds: Collection<UUID>) {
        val ttl = Duration.ofSeconds(jwtProperties.accessTokenValidityInSeconds)
        sessionIds.forEach { redisTemplate.opsForValue().set(revokedKey(it), "1", ttl) }
    }

    fun isRevoked(sessionId: UUID): Boolean = redisTemplate.hasKey(revokedKey(sessionId))

    fun saveRotation(sessionId: UUID, previousRefreshTokenHash: String, tokens: TokenResponse, ttl: Duration) {
        val value = listOf(tokens.tokenType, tokens.accessToken, tokens.refreshToken, tokens.refreshTokenExpiresIn.toString())
            .joinToString(SEPARATOR)
        redisTemplate.opsForValue().set(rotationKey(sessionId, previousRefreshTokenHash), value, ttl)
    }

    fun findRotation(sessionId: UUID, previousRefreshTokenHash: String): TokenResponse? {
        val parts = redisTemplate.opsForValue().get(rotationKey(sessionId, previousRefreshTokenHash))
            ?.split(SEPARATOR)
            ?.takeIf { it.size == 4 }
            ?: return null
        return TokenResponse(
            tokenType = parts[0],
            accessToken = parts[1],
            refreshToken = parts[2],
            refreshTokenExpiresIn = parts[3].toLongOrNull() ?: return null,
        )
    }

    fun saveLegacyMigration(userId: Long, legacyRefreshTokenHash: String, migrated: RotatedTokens, ttl: Duration) {
        val tokens = migrated.tokens
        val value = listOf(tokens.tokenType, tokens.accessToken, tokens.refreshToken, tokens.refreshTokenExpiresIn.toString(), migrated.deviceId)
            .joinToString(SEPARATOR)
        redisTemplate.opsForValue().set(legacyMigrationKey(userId, legacyRefreshTokenHash), value, ttl)
    }

    fun findLegacyMigration(userId: Long, legacyRefreshTokenHash: String): RotatedTokens? {
        val parts = redisTemplate.opsForValue().get(legacyMigrationKey(userId, legacyRefreshTokenHash))
            ?.split(SEPARATOR)
            ?.takeIf { it.size == 5 }
            ?: return null
        val tokens = TokenResponse(
            tokenType = parts[0],
            accessToken = parts[1],
            refreshToken = parts[2],
            refreshTokenExpiresIn = parts[3].toLongOrNull() ?: return null,
        )
        return RotatedTokens(tokens, parts[4])
    }

    fun findLegacyRefreshToken(userId: Long): String? = redisTemplate.opsForValue().get(legacyKey(userId))

    fun deleteLegacyRefreshToken(userId: Long) {
        redisTemplate.delete(legacyKey(userId))
    }

    private fun revokedKey(sessionId: UUID) = "auth:session:revoked:$sessionId"

    private fun rotationKey(sessionId: UUID, hash: String) = "auth:session:rotation:$sessionId:$hash"

    private fun legacyKey(userId: Long) = "refresh:$userId"

    private fun legacyMigrationKey(userId: Long, hash: String) = "auth:session:legacy-migration:$userId:$hash"

    companion object {
        private const val SEPARATOR = "\n"
    }
}
