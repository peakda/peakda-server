package com.peakda.server.domain.feed.presentation.v2.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이모지 키 오름차순의 반응 집계 페이지")
data class ReactionPageResponse(
    @field:Schema(description = "이모지별 반응 목록")
    val items: List<ReactionItemResponse>,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasMore: Boolean,
    @field:Schema(description = "다음 요청의 afterKey. 마지막 페이지는 null", example = "u:1f44d")
    val nextAfterKey: String?,
)
