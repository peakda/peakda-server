package com.peakda.server.domain.launchalert.presentation.response

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.entity.LaunchAlertSubscription
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "출시 알림 신청 1건")
data class LaunchAlertSubscriptionResponse(
    @field:Schema(description = "신청 id", example = "101")
    val id: Long,

    @field:Schema(description = "알림 받을 앱 플랫폼", example = "IOS")
    val platform: LaunchAlertPlatform,

    @field:Schema(description = "이메일", example = "ex1@xxx.com")
    val email: String,

    @field:Schema(description = "동의한 안내 문구 버전", example = "2026-10-21")
    val consentVersion: String,

    @field:Schema(description = "동의 시각. 재신청하면 갱신된다", example = "2026-10-21T03:12:45Z")
    val consentedAt: Instant,

    @field:Schema(description = "최초 신청 시각", example = "2026-10-21T03:12:45Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(subscription: LaunchAlertSubscription): LaunchAlertSubscriptionResponse =
            LaunchAlertSubscriptionResponse(
                id = requireNotNull(subscription.id),
                platform = subscription.platform,
                email = subscription.email,
                consentVersion = subscription.consentVersion,
                consentedAt = subscription.consentedAt,
                createdAt = subscription.createdAt,
            )
    }
}
