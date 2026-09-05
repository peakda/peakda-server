package com.peakda.server.domain.feed.presentation.response

import com.peakda.server.domain.spot.entity.ReactionType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "리액션 타입별 집계 1건")
data class ReactionCount(
    @field:Schema(description = "리액션 타입", example = "HEART")
    val reactionType: ReactionType,

    @field:Schema(description = "개수", example = "12")
    val count: Long,
)
