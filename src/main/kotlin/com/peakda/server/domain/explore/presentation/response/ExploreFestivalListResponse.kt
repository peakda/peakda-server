package com.peakda.server.domain.explore.presentation.response

import com.peakda.server.domain.explore.presentation.response.ExploreResponse.ExploreFestivalItem
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "오늘 진행 중인 꽃축제 전체 목록")
data class ExploreFestivalListResponse(
    @field:Schema(description = "꽃축제만 남긴 종료 임박순 목록")
    val items: List<ExploreFestivalItem>,
)
