package com.peakda.server.domain.auth.app.presentation.request

import com.peakda.server.domain.auth.session.presentation.request.SessionDeviceRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@Schema(description = "앱 토큰 재발급 요청")
data class AppTokenRefreshRequest(
    @field:NotBlank
    @field:Schema(description = "발급받은 리프레시 토큰")
    val refreshToken: String,

    @field:Valid
    @field:Schema(
        description = "로그인 기기 정보. 세션 도입 전에 받은 리프레시 토큰을 기기 세션으로 옮길 때 쓴다.",
        nullable = true,
    )
    val device: SessionDeviceRequest? = null,
)
