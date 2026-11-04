package com.peakda.server.domain.feed.presentation.v2.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이모지별 반응 집계")
data class ReactionItemResponse(
    @field:Schema(description = "이모지 식별 키", example = "u:1f44d")
    val key: String,
    @field:Schema(description = "표시할 이모지", example = "👍")
    val emoji: String,
    @field:Schema(description = "반응 사용자 수", example = "12")
    val count: Long,
    @field:Schema(description = "현재 사용자가 이 반응을 남겼는지 여부", example = "true")
    val reactedByMe: Boolean,
)
