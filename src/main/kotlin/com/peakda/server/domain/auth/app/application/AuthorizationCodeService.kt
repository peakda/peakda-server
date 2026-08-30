package com.peakda.server.domain.auth.app.application

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64

/**
 * 앱 로그인용 일회성 코드. 딥링크로 노출되는 값이라 짧은 TTL 로만 살아 있고,
 * 교환하는 순간 사라져 재사용되지 않는다.
 */
@Service
class AuthorizationCodeService(
    private val redisTemplate: StringRedisTemplate,
    private val properties: AppLoginProperties,
) {

    fun issue(payload: AuthorizationCodePayload): String {
        val code = generateCode()
        redisTemplate.opsForValue().set(key(code), payload.serialize(), properties.codeTtl)
        return code
    }

    /** 읽는 즉시 지운다. 같은 코드로 두 번 토큰을 받을 수 없다. */
    fun consume(code: String): AuthorizationCodePayload? =
        redisTemplate.opsForValue().getAndDelete(key(code))?.let(AuthorizationCodePayload::parse)

    private fun generateCode(): String {
        val bytes = ByteArray(CODE_BYTES).also(SECURE_RANDOM::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun key(code: String): String = KEY_PREFIX + code

    companion object {
        private const val KEY_PREFIX = "auth:code:"
        private const val CODE_BYTES = 32
        private val SECURE_RANDOM = SecureRandom()
    }
}
