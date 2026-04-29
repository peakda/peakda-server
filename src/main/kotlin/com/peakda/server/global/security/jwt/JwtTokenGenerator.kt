package com.peakda.server.global.security.jwt

interface JwtTokenGenerator {
    fun generateToken(userId: Long, email: String?, authorities: Collection<String>): TokenResponse
}
