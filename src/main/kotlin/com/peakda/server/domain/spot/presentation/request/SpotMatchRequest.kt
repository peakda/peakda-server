package com.peakda.server.domain.spot.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "스팟 매칭 요청 — 카카오 검색에서 받은 좌표/이름을 보낸다")
data class SpotMatchRequest(
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    @field:Schema(description = "위도 (WGS84)", example = "37.5665")
    val latitude: Double,

    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    @field:Schema(description = "경도 (WGS84)", example = "126.978")
    val longitude: Double,

    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "장소 이름 (LOCAL 스팟 생성 시 표시명으로 사용)", example = "남산")
    val name: String,

    @field:Size(max = 500)
    @field:Schema(description = "주소 (선택)", example = "서울 중구 남산공원길 105")
    val address: String? = null,

    @field:Size(max = 100)
    @field:Schema(description = "카카오 장소 id (있다면 전달, LOCAL 중복 탐지에 사용)", example = "27325497")
    val kakaoPlaceId: String? = null,
)
