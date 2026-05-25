package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "스팟 매칭 결과")
data class SpotMatchResponse(
    @field:Schema(description = "기존 스팟과 매칭되었는지 여부", example = "true")
    val matched: Boolean,

    @field:Schema(description = "매칭된 스팟 (없으면 null)")
    val spot: MatchedSpot?,

    @field:Schema(
        description = "제안되는 분류. 매칭 결과가 없으면 LOCAL 을 기본 제안",
        example = "ATTRACTION",
    )
    val suggestedType: SpotType,
) {
    @Schema(description = "매칭된 스팟 요약")
    data class MatchedSpot(
        @field:Schema(description = "스팟 PK", example = "1024")
        val id: Long,
        @field:Schema(description = "스팟 분류", example = "ATTRACTION")
        val type: SpotType,
        @field:Schema(description = "스팟 표시명", example = "남산")
        val name: String,
        @field:Schema(description = "스팟 주소", example = "서울 중구 남산공원길 105")
        val address: String?,
        @field:Schema(description = "ATTRACTION 일 때 attraction id", example = "501")
        val attractionId: Long?,
    )
}
