package com.peakda.server.domain.launchalert.presentation.request

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "앱 출시 알림 신청")
data class SubscribeLaunchAlertRequest(
    @field:Schema(description = "알림 받을 앱 플랫폼", example = "IOS")
    val platform: LaunchAlertPlatform,

    @field:NotBlank
    @field:Size(max = 254)
    // 따옴표 local part 나 =,+,-,@ 로 시작하는 주소를 받지 않는다. 관리자가 여는 CSV 에서 수식으로 실행될 수 있다.
    @field:Pattern(regexp = EMAIL_PATTERN, message = "이메일 형식이 올바르지 않습니다.")
    @field:Schema(description = "알림 받을 이메일 (소문자로 저장)", example = "ex1@xxx.com")
    val email: String,

    @field:Schema(
        description = "개인정보 수집·이용 동의 여부. true 가 아니면 LAUNCH_ALERT_CONSENT_REQUIRED 로 거절한다",
        example = "true",
    )
    val privacyConsent: Boolean,
) {
    companion object {
        private const val EMAIL_PATTERN =
            "^[A-Za-z0-9_][A-Za-z0-9._%+-]*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$"
    }
}
