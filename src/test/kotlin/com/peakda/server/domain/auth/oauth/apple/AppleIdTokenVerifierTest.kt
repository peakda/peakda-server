package com.peakda.server.domain.auth.oauth.apple

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Jwks
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Date

class AppleIdTokenVerifierTest {

    private val clientId = "com.peakda.app"
    private val keyId = "test-key-id"
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    /** Apple JWKS 엔드포인트가 [keyPair] 의 공개키를 [keyId] 로 내려주도록 응답을 세팅한다. */
    private fun verifierWithServer(): Pair<AppleIdTokenVerifier, MockRestServiceServer> {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(ExpectedCount.manyTimes(), requestTo(AppleProperties.JWKS_URL))
            .andRespond(withSuccess(jwksJson(), MediaType.APPLICATION_JSON))

        val publicKeyClient = ApplePublicKeyClient(builder)
        val verifier = AppleIdTokenVerifier(publicKeyClient, AppleProperties(clientId))
        return verifier to server
    }

    private fun jwksJson(): String {
        val jwk = Jwks.builder()
            .key(keyPair.public as RSAPublicKey)
            .id(keyId)
            .build()
        // Apple JWKS 는 {"keys":[ <공개 JWK> ]} 형태다. jjwt 는 단일 공개 JWK 직렬화(Jwks.json)만
        // public 으로 제공하므로 keys 배열로 감싸 동일 구조를 만든다.
        return """{"keys":[${Jwks.json(jwk)}]}"""
    }

    private fun signedToken(
        sub: String = "apple-sub-123",
        email: String? = "user@privaterelay.appleid.com",
        audience: String = clientId,
        issuer: String = AppleProperties.ISSUER,
        expiresAt: Instant = Instant.now().plusSeconds(600),
        kid: String = keyId,
    ): String = Jwts.builder()
        .header().keyId(kid).and()
        .issuer(issuer)
        .audience().add(audience).and()
        .subject(sub)
        .apply { email?.let { claim("email", it) } }
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(expiresAt))
        .signWith(keyPair.private as RSAPrivateKey)
        .compact()

    @Test
    fun `유효한 id_token 에서 sub 와 email 을 추출한다`() {
        val (verifier, _) = verifierWithServer()

        val claims = verifier.verify(signedToken())

        assertThat(claims.sub).isEqualTo("apple-sub-123")
        assertThat(claims.email).isEqualTo("user@privaterelay.appleid.com")
    }

    @Test
    fun `email 이 없는 토큰도 검증되고 email 은 null 이다`() {
        val (verifier, _) = verifierWithServer()

        val claims = verifier.verify(signedToken(email = null))

        assertThat(claims.sub).isEqualTo("apple-sub-123")
        assertThat(claims.email).isNull()
    }

    @Test
    fun `audience 가 client-id 와 다르면 예외를 던진다`() {
        val (verifier, _) = verifierWithServer()

        assertThatThrownBy { verifier.verify(signedToken(audience = "com.other.app")) }
            .isInstanceOf(AppleTokenInvalidException::class.java)
    }

    @Test
    fun `issuer 가 Apple 이 아니면 예외를 던진다`() {
        val (verifier, _) = verifierWithServer()

        assertThatThrownBy { verifier.verify(signedToken(issuer = "https://evil.example.com")) }
            .isInstanceOf(AppleTokenInvalidException::class.java)
    }

    @Test
    fun `만료된 토큰이면 예외를 던진다`() {
        val (verifier, _) = verifierWithServer()

        assertThatThrownBy { verifier.verify(signedToken(expiresAt = Instant.now().minusSeconds(60))) }
            .isInstanceOf(AppleTokenInvalidException::class.java)
    }

    @Test
    fun `다른 키로 서명된 토큰이면 예외를 던진다`() {
        val (verifier, _) = verifierWithServer()
        val attackerKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val forged = Jwts.builder()
            .header().keyId(keyId).and()
            .issuer(AppleProperties.ISSUER)
            .audience().add(clientId).and()
            .subject("apple-sub-123")
            .expiration(Date.from(Instant.now().plusSeconds(600)))
            .signWith(attackerKey.private as RSAPrivateKey)
            .compact()

        assertThatThrownBy { verifier.verify(forged) }
            .isInstanceOf(AppleTokenInvalidException::class.java)
    }

    @Test
    fun `알 수 없는 kid 면 예외를 던진다`() {
        val (verifier, _) = verifierWithServer()

        assertThatThrownBy { verifier.verify(signedToken(kid = "unknown-kid")) }
            .isInstanceOf(AppleTokenInvalidException::class.java)
    }
}
