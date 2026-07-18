package com.peakda.server.domain.notification.presentation.request

import com.peakda.server.domain.notification.entity.DevicePlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "디바이스 토큰 등록 요청")
data class RegisterDeviceRequest(
    @field:NotBlank
    @field:Schema(description = "푸시 알림 디바이스 토큰", example = "fcm-device-token-example")
    val token: String,

    @field:Schema(description = "디바이스 플랫폼", example = "ANDROID")
    val platform: DevicePlatform,
)
