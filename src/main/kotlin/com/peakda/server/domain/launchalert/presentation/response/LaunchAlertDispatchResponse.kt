package com.peakda.server.domain.launchalert.presentation.response

import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatch
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "출시 안내 메일 발송 1회")
data class LaunchAlertDispatchResponse(
    @field:Schema(description = "발송 id", example = "3")
    val id: Long,

    @field:Schema(description = "앱 플랫폼", example = "IOS")
    val platform: LaunchAlertPlatform,

    @field:Schema(description = "메일에 넣은 스토어 주소", example = "https://apps.apple.com/kr/app/id0000000000")
    val storeUrl: String,

    @field:Schema(description = "발송 상태. DISPATCHING 은 보내는 중, COMPLETED 는 끝까지 시도함, FAILED 는 연속 실패로 중단, CANCELED 는 관리자가 멈춤", example = "DISPATCHING")
    val status: LaunchAlertDispatchStatus,

    @field:Schema(description = "보낸 메일 수", example = "120")
    val sentCount: Int,

    @field:Schema(description = "보내지 못한 메일 수. 그 신청은 목록에 남는다", example = "2")
    val failedCount: Int,

    @field:Schema(description = "발송 요청 시각", example = "2026-11-02T01:00:00Z")
    val requestedAt: Instant,

    @field:Schema(description = "발송이 끝나거나 중단된 시각. 보내는 중이면 null", example = "2026-11-02T01:04:10Z", nullable = true)
    val completedAt: Instant?,
) {
    companion object {
        fun from(dispatch: LaunchAlertDispatch): LaunchAlertDispatchResponse =
            LaunchAlertDispatchResponse(
                id = requireNotNull(dispatch.id),
                platform = dispatch.platform,
                storeUrl = dispatch.storeUrl,
                status = dispatch.status,
                sentCount = dispatch.sentCount,
                failedCount = dispatch.failedCount,
                requestedAt = dispatch.createdAt,
                completedAt = dispatch.completedAt,
            )
    }
}
