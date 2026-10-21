package com.peakda.server.domain.launchalert.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "출시 알림 신청 파기 결과")
data class LaunchAlertPurgeResponse(
    @field:Schema(description = "파기한 신청 수", example = "2")
    val deletedCount: Int,
)
