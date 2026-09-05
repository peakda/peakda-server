package com.peakda.server.common.security.jwt

data class TokenResponse(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String
)
