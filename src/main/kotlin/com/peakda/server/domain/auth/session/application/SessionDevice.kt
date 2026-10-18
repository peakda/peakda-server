package com.peakda.server.domain.auth.session.application

import com.peakda.server.domain.auth.session.entity.SessionClientType
import com.peakda.server.domain.auth.session.entity.SessionPlatform
import java.util.UUID

/**
 * 세션을 만들 기기 정보. [deviceId] 가 같으면 같은 기기로 보고 기존 세션을 교체한다.
 * 앱 구버전처럼 식별자를 보내지 않으면 새 값을 만들어 매번 새 기기로 취급한다.
 */
data class SessionDevice(
    val deviceId: String,
    val deviceName: String?,
    val clientType: SessionClientType,
    val platform: SessionPlatform?,
) {
    companion object {
        const val MAX_LENGTH = 100
        private val DEVICE_ID_PATTERN = Regex("^[!-~]{1,$MAX_LENGTH}$")

        fun app(deviceId: String?, deviceName: String?, platform: SessionPlatform?): SessionDevice = SessionDevice(
            deviceId = normalizeId(deviceId),
            deviceName = deviceName?.trim()?.take(MAX_LENGTH)?.takeIf { it.isNotEmpty() },
            clientType = SessionClientType.APP,
            platform = platform,
        )

        /** 기기 정보를 보내지 않는 구버전 앱. */
        fun unknownApp(): SessionDevice = app(null, null, null)

        fun web(deviceId: String?, userAgent: String?): SessionDevice = SessionDevice(
            deviceId = normalizeId(deviceId),
            deviceName = WebDeviceNames.of(userAgent),
            clientType = SessionClientType.WEB,
            platform = null,
        )

        private fun normalizeId(deviceId: String?): String =
            deviceId?.takeIf { DEVICE_ID_PATTERN.matches(it) } ?: UUID.randomUUID().toString()
    }
}
