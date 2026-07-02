package com.peakda.server.domain.search.presentation.response

import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "스팟 검색 결과 1건")
data class SpotSearchItem(
    @field:Schema(description = "스팟 id", example = "100")
    val spotId: Long,

    @field:Schema(description = "스팟 유형", example = "ATTRACTION")
    val type: SpotType,

    @field:Schema(description = "스팟명", example = "남산")
    val name: String,

    @field:Schema(description = "주소 (없으면 null)", example = "서울 중구 남산공원길 105")
    val address: String?,

    @field:Schema(description = "위도", example = "37.5512")
    val latitude: Double,

    @field:Schema(description = "경도", example = "126.9882")
    val longitude: Double,
)
