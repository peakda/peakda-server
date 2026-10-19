package com.peakda.server.common.security.jwt

import java.util.UUID

/**
 * 서명·만료 검증을 통과한 토큰의 클레임.
 * [sessionId] 가 없으면 세션 도입 전에 발급된 토큰이다.
 */
data class JwtClaims(
    val userId: Long,
    val sessionId: UUID?,
    val refresh: Boolean,
)
