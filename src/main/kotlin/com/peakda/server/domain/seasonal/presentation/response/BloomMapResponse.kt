package com.peakda.server.domain.seasonal.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "지도 영역 내 Spot 핀별 개화 상태 (핀 3단계, ENDED 제외)")
data class BloomMapResponse(
    @field:Schema(description = "명소 개화 추정 산출 기준일 (추정 데이터가 없으면 null)", example = "2026-06-06")
    val baseDate: LocalDate?,

    @field:Schema(description = "핀 수", example = "12")
    val count: Int,

    @field:Schema(description = "Spot 핀 목록 (명소형 + 동네형)")
    val pins: List<BloomMapPin>,
) {
    @Schema(description = "지도 핀 1건과 그 꽃 슬롯들")
    data class BloomMapPin(
        @field:Schema(
            description = "스팟 id. 명소형은 Spot 행이 아직 없으면 null (탭 시 /api/spots/match 로 materialize), 동네형은 항상 존재",
            example = "100",
        )
        val spotId: Long?,

        @field:Schema(description = "연결된 명소 id (동네형이면 null)", example = "501")
        val attractionId: Long?,

        @field:Schema(description = "핀 유형", example = "ATTRACTION")
        val type: SpotType,

        @field:Schema(description = "핀 이름 (명소명 또는 스팟명)", example = "남산")
        val name: String,

        @field:Schema(description = "위도", example = "37.5512")
        val latitude: Double?,

        @field:Schema(description = "경도", example = "126.9882")
        val longitude: Double?,

        @field:Schema(description = "이 핀의 꽃 슬롯들")
        val blooms: List<BloomSlot>,
    )

    @Schema(description = "Spot×카테고리 현재 상태 슬롯")
    data class BloomSlot(
        @field:Schema(description = "꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "카테고리 표시명", example = "벚꽃")
        val displayName: String,

        @field:Schema(description = "현재 상태 (PREPARING/STARTED/PEAK)", example = "PEAK")
        val status: BloomStatus,

        @field:Schema(description = "신뢰도 (0~1)", example = "0.9")
        val confidence: Double,
    )
}
