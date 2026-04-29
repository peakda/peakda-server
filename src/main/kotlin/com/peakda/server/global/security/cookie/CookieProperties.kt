package com.peakda.server.global.security.cookie

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cookie")
data class CookieProperties(
    val domain: String?,
    val secure: Boolean,
    val sameSite: String,
    val path: String = "/",
    val accessTokenName: String = "access-token",
    val refreshTokenName: String = "refresh-token",
    val signupTokenName: String = "signup-token",
    val signupTokenValidityInSeconds: Long = 900,
)
