package com.peakda.server.domain.auth.app.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "앱 토큰 재발급 요청")
data class AppTokenRefreshRequest(
    @field:NotBlank
    @field:Schema(description = "발급받은 리프레시 토큰")
    val refreshToken: String,
)
