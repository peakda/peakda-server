package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "찜한 스팟")
data class SpotFavoriteResponse(
    @field:Schema(description = "스팟 PK", example = "1024")
    val spotId: Long,

    @field:Schema(description = "스팟 분류", example = "ATTRACTION")
    val type: SpotType,

    @field:Schema(description = "스팟 표시명", example = "여의도 한강공원")
    val name: String,

    @field:Schema(description = "스팟 주소", example = "서울 영등포구 여의동로 330")
    val address: String?,

    @field:Schema(description = "ATTRACTION 일 때 attraction id", example = "501")
    val attractionId: Long?,

    @field:Schema(description = "만개 알림 수신 여부", example = "true")
    val notifyEnabled: Boolean,

    @field:Schema(description = "찜한 시각", example = "2026-05-29T09:41:00Z")
    val favoritedAt: Instant,
)
