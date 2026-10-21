package com.peakda.server.domain.launchalert.presentation.response

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "출시 안내 발송 대상 전체")
data class LaunchAlertRecipientsResponse(
    @field:Schema(description = "앱 플랫폼", example = "IOS")
    val platform: LaunchAlertPlatform,

    @field:Schema(description = "대상 수", example = "2")
    val count: Int,

    @field:Schema(description = "신청 순 이메일 목록", example = "[\"ex1@xxx.com\", \"ex2@xxx.com\"]")
    val emails: List<String>,
)
