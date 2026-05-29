package com.peakda.server.domain.spot.presentation.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "찜한 스팟 만개 알림 설정 변경 요청")
data class UpdateFavoriteNotifyRequest(
    @field:Schema(description = "만개 알림 수신 여부", example = "true")
    val enabled: Boolean,
)
