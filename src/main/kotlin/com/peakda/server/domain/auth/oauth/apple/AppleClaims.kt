package com.peakda.server.domain.auth.oauth.apple

/** Apple id_token 검증 결과에서 추출한 사용자 식별 정보. */
data class AppleClaims(
    val sub: String,
    val email: String?,
)
