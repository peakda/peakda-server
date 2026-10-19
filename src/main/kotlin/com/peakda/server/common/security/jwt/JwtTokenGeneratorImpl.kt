package com.peakda.server.common.security.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
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
        const val SESSION_ID_KEY = "sid"
        const val TOKEN_TYPE_KEY = "typ"
        const val REFRESH_TOKEN_TYPE = "refresh"
    }

    @PostConstruct
    fun init() {
        val keyBytes = Decoders.BASE64.decode(jwtProperties.secret)
        this.key = Keys.hmacShaKeyFor(keyBytes)
        log.info("JwtTokenGenerator initialized")
    }

    override fun generateToken(
        userId: Long,
        email: String?,
        authorities: Collection<String>,
        sessionId: UUID,
        refreshTokenValidityInSeconds: Long,
    ): TokenResponse {
        val now = Instant.now()
        val accessTokenExpiresAt = now.plus(jwtProperties.accessTokenValidityInSeconds, ChronoUnit.SECONDS)
        val refreshTokenExpiresAt = now.plus(refreshTokenValidityInSeconds, ChronoUnit.SECONDS)

        val accessToken = Jwts.builder()
            .subject(userId.toString())
            .apply { if (email != null) claim(EMAIL_KEY, email) }
            .claim(AUTHORITIES_KEY, authorities.joinToString(","))
            .claim(SESSION_ID_KEY, sessionId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(accessTokenExpiresAt))
            .signWith(key)
            .compact()

        // jti 가 없으면 같은 초에 발급한 리프레시 토큰이 똑같아져 회전 전후를 구분하지 못한다.
        val refreshToken = Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId.toString())
            .claim(SESSION_ID_KEY, sessionId.toString())
            .claim(TOKEN_TYPE_KEY, REFRESH_TOKEN_TYPE)
            .issuedAt(Date.from(now))
            .expiration(Date.from(refreshTokenExpiresAt))
            .signWith(key)
            .compact()

        return TokenResponse(
            tokenType = BEARER,
            accessToken = accessToken,
            refreshToken = refreshToken,
            refreshTokenExpiresIn = refreshTokenValidityInSeconds,
        )
    }
}
