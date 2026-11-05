package com.peakda.server.domain.spot.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "외부 링크로 바로 연결하는 홍보 지도 핀. 스팟 프리뷰나 상세를 조회하지 않는다.")
data class PromotionalMapPinResponse(
    @field:Schema(description = "홍보 핀 식별자", example = "BUSAN_NORTH_PORT")
    val id: String,

    @field:Schema(description = "핀 이름", example = "부산 북항")
    val name: String,

    @field:Schema(description = "위도", example = "35.0")
    val latitude: Double,

    @field:Schema(description = "경도", example = "129.0")
    val longitude: Double,

    @field:Schema(description = "프론트 핀 컴포넌트 식별자. SHARK는 상어 핀이다.", example = "SHARK")
    val iconKey: String,

    @field:Schema(description = "핀을 누르면 바로 열 HTTP(S) URL", example = "https://example.com/north-port")
    val linkUrl: String,
)
