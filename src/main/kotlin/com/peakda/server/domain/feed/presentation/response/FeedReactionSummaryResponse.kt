package com.peakda.server.domain.feed.presentation.response

import com.peakda.server.domain.spot.entity.ReactionType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "피드 기록의 리액션 요약")
data class FeedReactionSummaryResponse(
    @field:Schema(description = "스팟 기록 id", example = "1024")
    val recordId: Long,

    @field:Schema(description = "리액션 타입별 집계")
    val counts: List<ReactionCount>,

    @field:Schema(description = "현재 로그인 사용자가 남긴 리액션 타입들 (없으면 빈 배열)")
    val myReactions: Set<ReactionType>,
)
