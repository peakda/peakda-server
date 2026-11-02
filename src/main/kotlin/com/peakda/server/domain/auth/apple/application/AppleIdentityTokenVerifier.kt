package com.peakda.server.domain.auth.apple.application

import com.peakda.server.common.exception.AuthorizationException
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.domain.auth.oauth.model.AppleOAuth2UserInfo
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.MessageDigest

/**
 * 앱이 Sign in with Apple 로 받은 identity token(JWT)을 검증한다.
 *
 * 서명은 애플 공개키(JWKS)로 확인한다. 디코더가 키를 캐시하고, 모르는 kid 가 오면 다시 받아온다.
 * 발급처(iss)·대상(aud = 앱 번들 ID)·만료(exp)와 함께 nonce 를 확인해, 다른 앱용 토큰이나
 * 가로챈 토큰을 그대로 다시 보내는 것을 막는다.
 */
class AppleIdentityTokenVerifier(
    private val decoder: JwtDecoder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun verify(identityToken: String, nonce: String): AppleOAuth2UserInfo {
        val jwt = try {
            decoder.decode(identityToken)
        } catch (e: JwtException) {
            log.info("Apple identity token rejected: {}", e.message)
            throw AuthorizationException(ErrorCode.APPLE_IDENTITY_TOKEN_INVALID)
        }
        // 앱은 원문 nonce 의 SHA-256(hex)을 애플 요청에 넣고, 애플은 그 값을 그대로 nonce 클레임에 담는다.
        // 서버는 원문을 받아 같은 해시가 토큰에 들어 있는지 본다.
        if (jwt.getClaimAsString(NONCE_CLAIM) != sha256Hex(nonce)) {
            log.info("Apple identity token rejected: nonce mismatch")
            throw AuthorizationException(ErrorCode.APPLE_IDENTITY_TOKEN_INVALID)
        }
        return AppleOAuth2UserInfo(jwt.claims)
    }

    companion object {
        const val ISSUER = "https://appleid.apple.com"
        private const val JWK_SET_URI = "https://appleid.apple.com/auth/keys"
        private const val NONCE_CLAIM = "nonce"

        fun create(bundleId: String): AppleIdentityTokenVerifier {
            val decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build()
            decoder.setJwtValidator(validator(bundleId))
            return AppleIdentityTokenVerifier(decoder)
        }

        /** 만료·발급처·대상 검증. 테스트가 다른 키로 서명한 디코더에도 같은 규칙을 건다. */
        fun validator(bundleId: String): OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(
            JwtValidators.createDefaultWithIssuer(ISSUER),
            JwtClaimValidator<Collection<String>?>(JwtClaimNames.AUD) { audience ->
                audience != null && bundleId in audience
            },
        )

        fun sha256Hex(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
