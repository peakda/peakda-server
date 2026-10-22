package com.peakda.server.domain.launchalert.presentation.request

import com.peakda.server.domain.launchalert.application.LaunchAlertMailContent
import com.peakda.server.domain.launchalert.application.UpdateLaunchAlertMailTemplateCommand
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "출시 안내 메일 문구 수정. HTML 은 쓸 수 없다(이스케이프된다)")
data class UpdateLaunchAlertMailTemplateRequest(
    @field:Schema(description = "앱 플랫폼", example = "IOS")
    val platform: LaunchAlertPlatform,

    @field:NotBlank
    @field:Size(max = 100)
    @field:Schema(description = "메일 제목", example = "피크다 iOS 앱이 출시됐어요")
    val subject: String,

    @field:NotBlank
    @field:Size(max = 100)
    @field:Schema(description = "본문 맨 위 큰 제목", example = "신청해 주신 iOS 앱이 출시됐어요")
    val headline: String,

    @field:NotBlank
    @field:Size(max = 2000)
    @field:Schema(description = "본문. 빈 줄로 문단을 나누고 줄바꿈은 그대로 살린다", example = "벚꽃부터 단풍까지, 전국 계절 명소의 지금 상태를 아이폰에서도 확인해 보세요.")
    val body: String,

    @field:NotBlank
    @field:Size(max = 30)
    @field:Schema(description = "스토어로 가는 버튼 문구", example = "App Store에서 받기")
    val buttonLabel: String,

    @field:NotBlank
    @field:Size(max = 500)
    @field:Schema(description = "하단 안내 문구 (수신 사유·파기 안내)", example = "이 메일은 iOS 출시 알림을 신청하신 분께 한 번만 보내 드립니다.")
    val footer: String,
) {
    fun toCommand() = UpdateLaunchAlertMailTemplateCommand(
        platform = platform,
        content = LaunchAlertMailContent(
            subject = subject,
            headline = headline,
            body = body,
            buttonLabel = buttonLabel,
            footer = footer,
        ),
    )
}
