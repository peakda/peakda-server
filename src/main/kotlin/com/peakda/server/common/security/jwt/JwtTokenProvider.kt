package com.peakda.server.common.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private val log = LoggerFactory.getLogger(this::class.java)
    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        val keyBytes = Decoders.BASE64.decode(jwtProperties.secret)
        this.key = Keys.hmacShaKeyFor(keyBytes)
    }

    /** 서명·만료를 검증하고 클레임을 꺼낸다. 검증에 실패하거나 subject 가 사용자 id 가 아니면 null. */
    fun parse(token: String): JwtClaims? {
        val payload = try {
            parser().parseSignedClaims(token).payload
        } catch (e: JwtException) {
            log.debug("JWT 검증 실패: {}", e.message)
            return null
        } catch (e: IllegalArgumentException) {
            log.debug("JWT 검증 실패: {}", e.message)
            return null
        }
        val userId = payload.subject?.toLongOrNull() ?: return null
        val sessionId = payload.get(JwtTokenGeneratorImpl.SESSION_ID_KEY, String::class.java)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        val refresh = payload.get(JwtTokenGeneratorImpl.TOKEN_TYPE_KEY, String::class.java) ==
            JwtTokenGeneratorImpl.REFRESH_TOKEN_TYPE
        return JwtClaims(userId = userId, sessionId = sessionId, refresh = refresh)
    }

    private fun parser() = Jwts.parser().verifyWith(key).build()
}
