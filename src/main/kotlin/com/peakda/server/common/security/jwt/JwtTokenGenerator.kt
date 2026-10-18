package com.peakda.server.common.security.jwt

import java.util.UUID

interface JwtTokenGenerator {
    fun generateToken(
        userId: Long,
        email: String?,
        authorities: Collection<String>,
        sessionId: UUID,
        refreshTokenValidityInSeconds: Long,
    ): TokenResponse
}
