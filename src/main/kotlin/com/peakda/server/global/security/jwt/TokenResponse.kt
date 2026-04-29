package com.peakda.server.global.security.jwt

data class TokenResponse(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String
)
