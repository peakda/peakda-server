package com.peakda.server.domain.search.presentation.response

import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "찜이 많은 순 인기 스팟 목록 (검색 화면 트렌딩)")
data class TrendingSpotsResponse(
    @field:Schema(description = "인기 스팟 목록 (찜 많은 순)")
    val items: List<TrendingSpotItem>,
) {
    @Schema(description = "인기 스팟 1건")
    data class TrendingSpotItem(
        @field:Schema(description = "스팟 id", example = "100")
        val spotId: Long,

        @field:Schema(description = "스팟 유형", example = "ATTRACTION")
        val type: SpotType,

        @field:Schema(description = "스팟명", example = "남산")
        val name: String,

        @field:Schema(description = "위도", example = "37.5512")
        val latitude: Double,

        @field:Schema(description = "경도", example = "126.9882")
        val longitude: Double,

        @field:Schema(description = "찜 수", example = "128")
        val favoriteCount: Long,
    )
}
