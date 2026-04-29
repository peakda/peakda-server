package com.peakda.server.global.security.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenGeneratorImpl(
    private val jwtProperties: JwtProperties
) : JwtTokenGenerator {

    private val log = LoggerFactory.getLogger(this::class.java)
    private lateinit var key: SecretKey

    companion object {
        private const val AUTHORITIES_KEY = "auth"
        private const val EMAIL_KEY = "email"
        private const val BEARER = "Bearer"
    }

    @PostConstruct
    fun init() {
        val keyBytes = Decoders.BASE64.decode(jwtProperties.secret)
        this.key = Keys.hmacShaKeyFor(keyBytes)
        log.info("JwtTokenGenerator initialized")
    }

    override fun generateToken(userId: Long, email: String?, authorities: Collection<String>): TokenResponse {
        val now = Instant.now()
        val accessTokenExpiresAt = now.plus(jwtProperties.accessTokenValidityInSeconds, ChronoUnit.SECONDS)
        val refreshTokenExpiresAt = now.plus(jwtProperties.refreshTokenValidityInSeconds, ChronoUnit.SECONDS)

        val accessToken = Jwts.builder()
            .subject(userId.toString())
            .apply { if (email != null) claim(EMAIL_KEY, email) }
            .claim(AUTHORITIES_KEY, authorities.joinToString(","))
            .issuedAt(Date.from(now))
            .expiration(Date.from(accessTokenExpiresAt))
            .signWith(key)
            .compact()

        val refreshToken = Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(refreshTokenExpiresAt))
            .signWith(key)
            .compact()

        return TokenResponse(
            tokenType = BEARER,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}
