package com.peakda.server.domain.spot.presentation.request

import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "스팟 기록 생성/수정 시 스팟 식별·생성 입력")
data class SpotInputRequest(
    @field:Schema(
        description = "이미 매칭되어 알려진 스팟 id. 있으면 다른 필드는 무시되고 그 스팟을 사용한다.",
        example = "1024",
    )
    val existingSpotId: Long? = null,

    @field:Schema(description = "사용자가 토글한 최종 분류 (ATTRACTION/LOCAL)", example = "LOCAL")
    val type: SpotType,

    @field:Schema(description = "ATTRACTION 일 때 attraction id", example = "501")
    val attractionId: Long? = null,

    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "스팟 표시명", example = "남산")
    val name: String,

    @field:Size(max = 500)
    @field:Schema(description = "주소 (선택)", example = "서울 중구 남산공원길 105")
    val address: String? = null,

    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    @field:Schema(description = "위도", example = "37.5665")
    val latitude: Double,

    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    @field:Schema(description = "경도", example = "126.978")
    val longitude: Double,

    @field:Size(max = 100)
    @field:Schema(description = "카카오 장소 id (선택, LOCAL 중복 탐지에 사용)", example = "27325497")
    val kakaoPlaceId: String? = null,
)
