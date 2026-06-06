package com.peakda.server.domain.seasonal.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "지도 영역 내 명소별 현재 개화 상태 (핀 3단계, ENDED 제외)")
data class BloomMapResponse(
    @field:Schema(description = "상태 산출 기준일 (없으면 null)", example = "2026-06-06")
    val baseDate: LocalDate?,

    @field:Schema(description = "명소 수", example = "12")
    val count: Int,

    @field:Schema(description = "명소 목록")
    val attractions: List<BloomMapItem>,
) {
    @Schema(description = "명소 1건과 그 꽃 슬롯들")
    data class BloomMapItem(
        @field:Schema(description = "명소 id", example = "501")
        val attractionId: Long,
        @field:Schema(description = "명소명", example = "남산")
        val title: String,
        @field:Schema(description = "위도", example = "37.5512")
        val latitude: Double?,
        @field:Schema(description = "경도", example = "126.9882")
        val longitude: Double?,
        @field:Schema(description = "이 명소의 꽃 슬롯들")
        val blooms: List<BloomSlot>,
    )

    @Schema(description = "명소×카테고리 현재 상태 슬롯")
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
