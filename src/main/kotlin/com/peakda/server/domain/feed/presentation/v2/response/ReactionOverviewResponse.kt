package com.peakda.server.domain.feed.presentation.v2.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "반응 수 내림차순·키 오름차순의 최대 8종 미리보기")
data class ReactionOverviewResponse(
    @field:Schema(description = "실제로 남겨진 이모지별 반응 목록")
    val items: List<ReactionItemResponse>,
    @field:Schema(description = "미리보기에 포함되지 않은 반응 종류가 있는지 여부", example = "false")
    val hasMore: Boolean,
)
