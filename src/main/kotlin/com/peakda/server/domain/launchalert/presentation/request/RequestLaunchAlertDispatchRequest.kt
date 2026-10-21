package com.peakda.server.domain.launchalert.presentation.request

import com.peakda.server.domain.launchalert.application.RequestLaunchAlertDispatchCommand
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

@Schema(description = "출시 안내 메일 발송 요청")
data class RequestLaunchAlertDispatchRequest(
    @field:Schema(description = "앱 플랫폼", example = "IOS")
    val platform: LaunchAlertPlatform,

    @field:Pattern(regexp = APP_STORE_URL_PATTERN, message = "App Store 주소(https://apps.apple.com/...)를 입력해 주세요.")
    @field:Schema(description = "메일에 넣을 App Store 주소", example = "https://apps.apple.com/kr/app/id0000000000")
    val storeUrl: String,
) {
    fun toCommand() = RequestLaunchAlertDispatchCommand(platform = platform, storeUrl = storeUrl.trim())
}

/** 메일 HTML 에 그대로 들어가므로 따옴표·꺾쇠가 섞이지 않는 문자만 받는다. */
internal const val APP_STORE_URL_PATTERN = "^\\s*https://apps\\.apple\\.com/[A-Za-z0-9/._~%?=&-]+\\s*$"
