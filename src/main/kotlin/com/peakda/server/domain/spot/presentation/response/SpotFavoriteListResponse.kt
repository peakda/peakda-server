package com.peakda.server.domain.spot.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "찜한 스팟 목록")
data class SpotFavoriteListResponse(
    @field:Schema(description = "찜한 스팟 총 개수", example = "8")
    val count: Int,

    @field:Schema(description = "찜한 스팟 목록 (최근 찜한 순)")
    val favorites: List<SpotFavoriteResponse>,
)
