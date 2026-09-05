package com.peakda.server.domain.location.presentation.response

import com.peakda.server.domain.location.entity.LocationAccessChannel
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.entity.LocationUsageLog
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "위치정보 이용·제공사실 확인자료 1건")
data class LocationUsageLogResponse(
    @field:Schema(description = "확인자료 id", example = "1001")
    val id: Long,

    @field:Schema(description = "대상 사용자 id", example = "7")
    val userId: Long,

    @field:Schema(description = "대상 사용자 이메일. 이메일이 없거나 탈퇴한 계정은 null", example = "ex1@xxx.com", nullable = true)
    val email: String?,

    @field:Schema(description = "대상 사용자 닉네임. 조회 시점에 계정이 없으면 null", example = "피크다", nullable = true)
    val nickname: String?,

    @field:Schema(description = "취득경로", example = "ANDROID")
    val channel: LocationAccessChannel,

    @field:Schema(description = "제공서비스", example = "BLOOM_MAP")
    val service: LocationServiceType,

    @field:Schema(description = "이용일시", example = "2026-08-17T12:49:24Z")
    val usedAt: Instant,
) {
    companion object {
        fun from(usageLog: LocationUsageLog, email: String?, nickname: String?): LocationUsageLogResponse =
            LocationUsageLogResponse(
                id = requireNotNull(usageLog.id),
                userId = usageLog.userId,
                email = email,
                nickname = nickname,
                channel = usageLog.channel,
                service = usageLog.service,
                usedAt = usageLog.usedAt,
            )
    }
}
