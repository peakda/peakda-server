package com.peakda.server.domain.auth.oauth.apple

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Apple 네이티브 로그인(id_token 검증) 설정.
 *
 * [clientId] 는 id_token 의 `aud` 클레임과 일치해야 하는 Apple 앱의 Bundle ID(또는 Services ID)다.
 * issuer / JWKS URL 은 Apple 고정값이므로 상수로 둔다.
 */
@ConfigurationProperties(prefix = "app.apple")
data class AppleProperties(
    val clientId: String,
) {
    companion object {
        const val ISSUER = "https://appleid.apple.com"
        const val JWKS_URL = "https://appleid.apple.com/auth/keys"
    }
}
