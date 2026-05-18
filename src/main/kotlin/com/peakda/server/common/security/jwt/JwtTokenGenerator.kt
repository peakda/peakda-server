package com.peakda.server.common.security.jwt

interface JwtTokenGenerator {
    fun generateToken(userId: Long, email: String?, authorities: Collection<String>): TokenResponse
}
