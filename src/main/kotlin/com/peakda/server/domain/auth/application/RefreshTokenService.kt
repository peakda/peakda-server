package com.peakda.server.domain.auth.application

import com.peakda.server.global.security.jwt.JwtProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Redis 기반 Refresh Token 관리 서비스
 * 토큰 탈취 시 서버 측에서 무효화할 수 있도록 Redis에 저장
 */
@Service
class RefreshTokenService(
    private val redisTemplate: StringRedisTemplate,
    private val jwtProperties: JwtProperties
) {

    companion object {
        private const val KEY_PREFIX = "refresh:"
    }

    fun saveRefreshToken(userId: Long, refreshToken: String) {
        val key = KEY_PREFIX + userId
        redisTemplate.opsForValue().set(key, refreshToken, jwtProperties.refreshTokenValidityInSeconds, TimeUnit.SECONDS)
    }

    fun getRefreshToken(userId: Long): String? {
        val key = KEY_PREFIX + userId
        return redisTemplate.opsForValue().get(key)
    }

    fun deleteRefreshToken(userId: Long) {
        val key = KEY_PREFIX + userId
        redisTemplate.delete(key)
    }

    fun rotateRefreshToken(userId: Long, newRefreshToken: String) {
        saveRefreshToken(userId, newRefreshToken)
    }
}
