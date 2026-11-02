package com.peakda.server.domain.auth.apple.application

import io.jsonwebtoken.Jwts
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Clock
import java.time.Duration
import java.util.Base64
import java.util.Date

/**
 * 애플 API(토큰 교환·폐기)에 보내는 client_secret.
 * 애플은 고정 문자열 대신 Sign in with Apple 키(.p8)로 ES256 서명한 JWT 를 받는다.
 */
class AppleClientSecretGenerator(
    private val teamId: String,
    private val keyId: String,
    private val clientId: String,
    private val privateKey: PrivateKey,
    private val clock: Clock = Clock.systemUTC(),
) {

    fun generate(): String {
        val now = clock.instant()
        return Jwts.builder()
            .header().keyId(keyId).and()
            .issuer(teamId)
            .subject(clientId)
            // 애플은 aud 를 배열이 아닌 문자열로 받는다.
            .audience().single(AppleIdentityTokenVerifier.ISSUER)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(VALIDITY)))
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact()
    }

    companion object {
        // 호출할 때마다 새로 만든다. 애플 허용 최대치(6개월)까지 오래 둘 이유가 없다.
        private val VALIDITY = Duration.ofMinutes(5)

        fun from(properties: AppleProperties): AppleClientSecretGenerator = AppleClientSecretGenerator(
            teamId = requireNotNull(properties.teamId),
            keyId = requireNotNull(properties.keyId),
            clientId = properties.bundleId,
            privateKey = parsePrivateKey(readPem(properties)),
        )

        private fun readPem(properties: AppleProperties): String =
            properties.privateKeyBase64?.takeIf { it.isNotBlank() }
                ?.let { String(Base64.getDecoder().decode(it.trim()), Charsets.UTF_8) }
                ?: properties.privateKeyLocation?.takeIf { it.isNotBlank() }
                    ?.let { Files.readString(Path.of(it.replaceFirst("~", System.getProperty("user.home")))) }
                ?: error("애플 개인키가 없다")

        /** 애플이 내려주는 .p8 은 PKCS#8 PEM 이다. */
        fun parsePrivateKey(pem: String): PrivateKey {
            val der = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
            return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(der)))
        }
    }
}
