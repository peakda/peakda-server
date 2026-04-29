package com.peakda.server.global.security.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
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

    fun validateToken(token: String): Boolean {
        return try {
            parser().parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            log.debug("JWT 검증 실패: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            log.debug("JWT 검증 실패: {}", e.message)
            false
        }
    }

    fun getUserIdFromToken(token: String): Long? {
        return try {
            parser().parseSignedClaims(token).payload.subject?.toLongOrNull()
        } catch (e: JwtException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun parser() = Jwts.parser().verifyWith(key).build()
}
