package com.peakda.server.domain.search.presentation.response

import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.BloomBadge
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

    @field:Schema(description = "대표 이미지 URL. 명소 대표 이미지가 없으면 최근 게시 기록 사진, 없으면 null", example = "https://img.peakda.kr/spot.jpg")
    val thumbnailUrl: String?,

    @field:Schema(description = "현재 개화 정보. 해당 정보가 없으면 null")
    val bloom: BloomBadge?,

    @field:Schema(description = "현재 사용자가 찜한 스팟인지 여부", example = "true")
    val favorited: Boolean,

    @field:Schema(description = "현재 사용자의 찜 알림 활성화 여부", example = "true")
    val notifyEnabled: Boolean,
)
