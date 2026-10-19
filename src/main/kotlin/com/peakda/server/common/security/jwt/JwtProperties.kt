package com.peakda.server.common.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * [refreshTokenValidityInSeconds] 는 웹(쿠키) 세션, [appRefreshTokenValidityInSeconds] 는 앱 세션의 리프레시 토큰 유효기간이다.
 * 앱은 한동안 열지 않아도 로그인이 유지돼야 하므로 더 길게 둔다. 둘 다 재발급할 때마다 다시 연장된다.
 */
@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenValidityInSeconds: Long,
    val refreshTokenValidityInSeconds: Long,
    val appRefreshTokenValidityInSeconds: Long = 2_592_000,
)
