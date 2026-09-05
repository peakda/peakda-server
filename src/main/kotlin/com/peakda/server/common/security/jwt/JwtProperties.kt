package com.peakda.server.common.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenValidityInSeconds: Long,
    val refreshTokenValidityInSeconds: Long,
)
