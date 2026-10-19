package com.peakda.server.domain.auth.session.presentation.response

import com.peakda.server.domain.auth.session.entity.AuthSession
import com.peakda.server.domain.auth.session.entity.SessionClientType
import com.peakda.server.domain.auth.session.entity.SessionPlatform
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "로그인된 기기")
data class DeviceSessionResponse(
    @field:Schema(description = "기기(세션) id. 해제 API 에 쓴다.", example = "3f2b8c1e-6a7d-4f0e-9c2a-1b5d8e7f9a10")
    val sessionId: UUID,

    @field:Schema(description = "기기 표시 이름. 구버전 앱이면 null", example = "SM-S918N", nullable = true)
    val deviceName: String?,

    @field:Schema(description = "로그인 경로", example = "APP")
    val clientType: SessionClientType,

    @field:Schema(description = "앱 플랫폼. 웹이거나 알 수 없으면 null", example = "ANDROID", nullable = true)
    val platform: SessionPlatform?,

    @field:Schema(description = "기기 등록(로그인) 시각", example = "2026-10-18T09:30:00Z")
    val registeredAt: Instant,

    @field:Schema(description = "마지막 사용(로그인·토큰 재발급) 시각", example = "2026-10-18T12:00:00Z")
    val lastUsedAt: Instant,

    @field:Schema(description = "지금 요청을 보낸 기기인지", example = "true")
    val current: Boolean,
) {
    companion object {
        fun from(session: AuthSession, current: Boolean) = DeviceSessionResponse(
            sessionId = session.id,
            deviceName = session.deviceName,
            clientType = session.clientType,
            platform = session.platform,
            registeredAt = session.createdAt,
            lastUsedAt = session.lastUsedAt,
            current = current,
        )
    }
}
