package com.peakda.server.domain.auth.application

import com.peakda.server.common.security.jwt.TokenResponse

/** 회전 결과와 그 세션의 기기 식별자. 웹은 이 값으로 device-id 쿠키를 맞춘다. */
data class RotatedTokens(
    val tokens: TokenResponse,
    val deviceId: String,
)
