package com.peakda.server.domain.auth.oauth.apple

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.LocatorAdapter
import io.jsonwebtoken.ProtectedHeader
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.Key

/**
 * Apple 네이티브 로그인의 id_token(JWT)을 검증한다.
 *
 * 1. 헤더의 `kid` 로 [ApplePublicKeyClient] 에서 서명 공개키를 찾아 서명을 검증한다.
 * 2. `iss == https://appleid.apple.com`, `aud == app.apple.client-id`, 만료 여부를 검증한다.
 * 3. `sub`(providerId)·`email` 을 추출해 [AppleClaims] 로 반환한다.
 *
 * 검증에 실패하면 [AppleTokenInvalidException] 을 던진다.
 */
@Component
class AppleIdTokenVerifier(
    private val applePublicKeyClient: ApplePublicKeyClient,
    private val appleProperties: AppleProperties,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun verify(identityToken: String): AppleClaims {
        val claims = parseClaims(identityToken)
        return AppleClaims(
            sub = claims.subject ?: throw AppleTokenInvalidException(),
            email = claims["email"] as? String,
        )
    }

    private fun parseClaims(identityToken: String): Claims {
        try {
            return Jwts.parser()
                .keyLocator(keyLocator)
                .requireIssuer(AppleProperties.ISSUER)
                .requireAudience(appleProperties.clientId)
                .build()
                .parseSignedClaims(identityToken)
                .payload
        } catch (e: JwtException) {
            log.debug("Apple id_token 검증 실패: {}", e.message)
            throw AppleTokenInvalidException()
        } catch (e: IllegalArgumentException) {
            log.debug("Apple id_token 검증 실패: {}", e.message)
            throw AppleTokenInvalidException()
        }
    }

    private val keyLocator = object : LocatorAdapter<Key>() {
        override fun locate(header: ProtectedHeader): Key? =
            header.keyId?.let { applePublicKeyClient.findPublicKey(it) }
    }
}
