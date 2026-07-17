package com.peakda.server.domain.notification.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "안 읽은 알림 개수 (뱃지용)")
data class UnreadCountResponse(
    @field:Schema(description = "안 읽은 알림 개수", example = "3")
    val unreadCount: Long,
)
