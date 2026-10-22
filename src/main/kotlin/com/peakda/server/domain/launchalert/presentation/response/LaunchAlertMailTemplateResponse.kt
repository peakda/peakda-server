package com.peakda.server.domain.launchalert.presentation.response

import com.peakda.server.domain.launchalert.entity.LaunchAlertMailTemplate
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "출시 안내 메일 문구")
data class LaunchAlertMailTemplateResponse(
    @field:Schema(description = "앱 플랫폼", example = "IOS")
    val platform: LaunchAlertPlatform,

    @field:Schema(description = "메일 제목", example = "피크다 iOS 앱이 출시됐어요")
    val subject: String,

    @field:Schema(description = "본문 맨 위 큰 제목", example = "신청해 주신 iOS 앱이 출시됐어요")
    val headline: String,

    @field:Schema(description = "본문. 빈 줄로 문단을 나눈다", example = "벚꽃부터 단풍까지, 전국 계절 명소의 지금 상태를 아이폰에서도 확인해 보세요.")
    val body: String,

    @field:Schema(description = "스토어로 가는 버튼 문구", example = "App Store에서 받기")
    val buttonLabel: String,

    @field:Schema(description = "하단 안내 문구", example = "이 메일은 iOS 출시 알림을 신청하신 분께 한 번만 보내 드립니다.")
    val footer: String,

    @field:Schema(description = "마지막 수정 시각", example = "2026-10-22T03:00:00Z")
    val updatedAt: Instant,
) {
    companion object {
        fun from(template: LaunchAlertMailTemplate) = LaunchAlertMailTemplateResponse(
            platform = template.platform,
            subject = template.subject,
            headline = template.headline,
            body = template.body,
            buttonLabel = template.buttonLabel,
            footer = template.footer,
            updatedAt = template.updatedAt,
        )
    }
}
