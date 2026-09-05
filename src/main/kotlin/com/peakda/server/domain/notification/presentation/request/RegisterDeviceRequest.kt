package com.peakda.server.domain.notification.presentation.request

import com.peakda.server.domain.notification.entity.DevicePlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "디바이스 토큰 등록 요청")
data class RegisterDeviceRequest(
    @field:NotBlank
    @field:Size(max = 1024)
    @field:Pattern(regexp = "^[!-~]+$")
    @field:Schema(description = "푸시 알림 디바이스 토큰 (ASCII, 최대 1024자)", example = "fcm-device-token-example")
    val token: String,

    @field:Schema(description = "디바이스 플랫폼", example = "ANDROID")
    val platform: DevicePlatform,
)
