package com.peakda.server.domain.auth.apple.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date

class AppleIdentityTokenVerifierTest {

    private val verifier = AppleIdentityTokenVerifier(
        NimbusJwtDecoder.withPublicKey(APPLE_KEYS.public as RSAPublicKey).build()
            .apply { setJwtValidator(AppleIdentityTokenVerifier.validator(BUNDLE_ID)) },
    )

    @Test
    fun `애플이 서명한 우리 앱용 토큰이면 애플 사용자 식별자와 이메일을 돌려준다`() {
        val userInfo = verifier.verify(token(), NONCE)

        assertThat(userInfo.getProviderId()).isEqualTo(APPLE_USER_ID)
        assertThat(userInfo.getEmail()).isEqualTo(EMAIL)
        assertThat(userInfo.getProfileImageUrl()).isNull()
    }

    @Test
    fun `다른 앱용으로 발급된 토큰은 거부한다`() {
        assertInvalid(token(audience = "com.other.app"), NONCE)
    }

    @Test
    fun `애플이 아닌 발급처의 토큰은 거부한다`() {
        assertInvalid(token(issuer = "https://evil.example.com"), NONCE)
    }

    @Test
    fun `만료된 토큰은 거부한다`() {
        assertInvalid(token(expiresAt = Instant.now().minusSeconds(120)), NONCE)
    }

    @Test
    fun `애플 키가 아닌 키로 서명한 토큰은 거부한다`() {
        assertInvalid(token(signingKey = rsaKeyPair().private), NONCE)
    }

    @Test
    fun `nonce 원문의 해시가 토큰과 다르면 거부한다`() {
        assertInvalid(token(), "other-nonce")
    }

    @Test
    fun `nonce 가 없는 토큰은 거부한다`() {
        assertInvalid(token(nonce = null), NONCE)
    }

    @Test
    fun `JWT 형식이 아니면 거부한다`() {
        assertInvalid("not-a-jwt", NONCE)
    }

    private fun assertInvalid(identityToken: String, nonce: String) {
        assertThatThrownBy { verifier.verify(identityToken, nonce) }
            .isInstanceOf(AuthorizationException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.APPLE_IDENTITY_TOKEN_INVALID)
    }

    private fun token(
        issuer: String = AppleIdentityTokenVerifier.ISSUER,
        audience: String = BUNDLE_ID,
        expiresAt: Instant = Instant.now().plusSeconds(600),
        nonce: String? = AppleIdentityTokenVerifier.sha256Hex(NONCE),
        signingKey: PrivateKey = APPLE_KEYS.private,
    ): String = Jwts.builder()
        .header().keyId("apple-key").and()
        .issuer(issuer)
        .subject(APPLE_USER_ID)
        // 애플 identity token 의 aud 는 문자열이다.
        .audience().single(audience)
        .issuedAt(Date.from(Instant.now().minusSeconds(10)))
        .expiration(Date.from(expiresAt))
        .claim("email", EMAIL)
        .apply { if (nonce != null) claim("nonce", nonce) }
        .signWith(signingKey, Jwts.SIG.RS256)
        .compact()

    companion object {
        private const val BUNDLE_ID = "com.peakda.app"
        private const val APPLE_USER_ID = "001234.0123456789abcdef0123456789abcdef.0123"
        private const val EMAIL = "abc123@privaterelay.appleid.com"
        private const val NONCE = "raw-nonce-from-app"
        private val APPLE_KEYS = rsaKeyPair()

        private fun rsaKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    }
}
