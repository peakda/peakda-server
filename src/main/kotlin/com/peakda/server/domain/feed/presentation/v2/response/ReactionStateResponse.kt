package com.peakda.server.domain.feed.presentation.v2.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "추가·삭제한 단일 이모지의 현재 반응 상태")
data class ReactionStateResponse(
    @field:Schema(description = "기록 ID", example = "1024")
    val recordId: Long,
    @field:Schema(description = "이모지 식별 키", example = "u:1f44d")
    val key: String,
    @field:Schema(description = "표시할 이모지", example = "👍")
    val emoji: String,
    @field:Schema(description = "반응 사용자 수. 마지막 삭제 시 0", example = "12")
    val count: Long,
    @field:Schema(description = "현재 사용자의 반응 여부", example = "true")
    val reactedByMe: Boolean,
)
