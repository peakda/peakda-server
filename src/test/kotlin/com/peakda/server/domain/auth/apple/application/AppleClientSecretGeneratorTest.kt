package com.peakda.server.domain.auth.apple.application

import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64

class AppleClientSecretGeneratorTest {

    @Test
    fun `애플이 요구하는 형식의 client secret 을 ES256 으로 서명한다`() {
        val generator = AppleClientSecretGenerator(TEAM_ID, KEY_ID, BUNDLE_ID, KEYS.private, FIXED_CLOCK)

        val secret = generator.generate()

        val parsed = Jwts.parser().verifyWith(KEYS.public).clock { java.util.Date.from(NOW) }.build()
            .parseSignedClaims(secret)
        assertThat(parsed.header.algorithm).isEqualTo("ES256")
        assertThat(parsed.header.keyId).isEqualTo(KEY_ID)
        assertThat(parsed.payload.issuer).isEqualTo(TEAM_ID)
        assertThat(parsed.payload.subject).isEqualTo(BUNDLE_ID)
        assertThat(parsed.payload.issuedAt.toInstant()).isEqualTo(NOW)
        assertThat(parsed.payload.expiration.toInstant()).isEqualTo(NOW.plusSeconds(300))
        // 애플은 aud 를 배열이 아닌 문자열로 받는다.
        val payloadJson = String(Base64.getUrlDecoder().decode(secret.split(".")[1]))
        assertThat(payloadJson).contains("\"aud\":\"https://appleid.apple.com\"")
    }

    @Test
    fun `base64 로 넣은 개인키를 읽는다`() {
        val properties = enabledProperties(privateKeyBase64 = Base64.getEncoder().encodeToString(pem().toByteArray()))

        assertSignedWithTestKey(AppleClientSecretGenerator.from(properties))
    }

    @Test
    fun `파일 경로로 넣은 개인키를 읽는다`(@TempDir dir: Path) {
        val keyFile = dir.resolve("AuthKey_TEST.p8").also { Files.writeString(it, pem()) }
        val properties = enabledProperties(privateKeyLocation = keyFile.toString())

        assertSignedWithTestKey(AppleClientSecretGenerator.from(properties))
    }

    @Test
    fun `켜 두고 키를 빠뜨리면 기동 단계에서 실패한다`() {
        assertThatThrownBy { AppleProperties(enabled = true, teamId = TEAM_ID, keyId = KEY_ID) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("APPLE_PRIVATE_KEY")
        assertThatThrownBy { AppleProperties(enabled = true, keyId = KEY_ID, privateKeyBase64 = "x") }
            .hasMessageContaining("APPLE_TEAM_ID")
    }

    @Test
    fun `꺼져 있으면 키 없이도 설정을 만들 수 있다`() {
        assertThat(AppleProperties().enabled).isFalse()
    }

    private fun assertSignedWithTestKey(generator: AppleClientSecretGenerator) {
        val claims = Jwts.parser().verifyWith(KEYS.public).build().parseSignedClaims(generator.generate()).payload
        assertThat(claims.issuer).isEqualTo(TEAM_ID)
    }

    private fun enabledProperties(privateKeyBase64: String? = null, privateKeyLocation: String? = null) = AppleProperties(
        enabled = true,
        teamId = TEAM_ID,
        keyId = KEY_ID,
        bundleId = BUNDLE_ID,
        privateKeyBase64 = privateKeyBase64,
        privateKeyLocation = privateKeyLocation,
    )

    /** 애플이 내려주는 .p8 과 같은 PKCS#8 PEM (64자 줄바꿈). */
    private fun pem(): String {
        val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(KEYS.private.encoded)
        return "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----\n"
    }

    companion object {
        private const val TEAM_ID = "TEAM123456"
        private const val KEY_ID = "KEY1234567"
        private const val BUNDLE_ID = "com.peakda.app"
        private val NOW: Instant = Instant.parse("2026-11-01T00:00:00Z")
        private val FIXED_CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
        private val KEYS: KeyPair = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair()
    }
}
