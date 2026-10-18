package com.peakda.server.domain.auth.session.presentation.request

import com.peakda.server.domain.auth.session.application.SessionDevice
import com.peakda.server.domain.auth.session.entity.SessionPlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "앱 로그인 기기 정보. 기기 관리 목록에 표시되고, 같은 deviceId 로 다시 로그인하면 기기 수가 늘지 않는다.")
data class SessionDeviceRequest(
    @field:Size(max = SessionDevice.MAX_LENGTH)
    @field:Pattern(regexp = "^[!-~]+$")
    @field:Schema(
        description = "앱 설치 시 만들어 보관하는 기기 식별자 (ASCII, 최대 100자). 앱을 지웠다 다시 깔기 전까지 바뀌지 않아야 한다.",
        example = "8f14e45f-ceea-467a-9575-4d6c1b7a2c11",
    )
    val deviceId: String? = null,

    @field:Size(max = SessionDevice.MAX_LENGTH)
    @field:Schema(description = "기기 표시 이름 (Android: Build.MODEL)", example = "SM-S918N")
    val deviceName: String? = null,

    @field:Schema(description = "앱 플랫폼", example = "ANDROID")
    val platform: SessionPlatform? = null,
) {
    fun toSessionDevice(): SessionDevice = SessionDevice.app(deviceId, deviceName, platform)

    companion object {
        /** 기기 정보를 보내지 않는 구버전 앱. */
        fun unknownApp(): SessionDevice = SessionDevice.app(null, null, null)
    }
}
