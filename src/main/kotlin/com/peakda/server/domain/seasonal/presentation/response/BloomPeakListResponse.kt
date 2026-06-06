package com.peakda.server.domain.seasonal.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "지금이 절정인 명소 목록 (status=PEAK)")
data class BloomPeakListResponse(
    @field:Schema(description = "상태 산출 기준일 (없으면 null)", example = "2026-06-06")
    val baseDate: LocalDate?,

    @field:Schema(description = "절정 명소 수", example = "7")
    val count: Int,

    @field:Schema(description = "절정 명소 목록")
    val items: List<BloomPeakItem>,
) {
    @Schema(description = "절정 명소 1건")
    data class BloomPeakItem(
        @field:Schema(description = "명소 id", example = "501")
        val attractionId: Long,
        @field:Schema(description = "명소명", example = "진해 여좌천")
        val title: String,
        @field:Schema(description = "위도", example = "35.1533")
        val latitude: Double?,
        @field:Schema(description = "경도", example = "128.6712")
        val longitude: Double?,
        @field:Schema(description = "꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,
        @field:Schema(description = "카테고리 표시명", example = "벚꽃")
        val displayName: String,
        @field:Schema(description = "신뢰도 (0~1)", example = "0.95")
        val confidence: Double,
        @field:Schema(description = "절정 시작일", example = "2026-04-01")
        val peakStartDate: LocalDate?,
        @field:Schema(description = "절정 종료일", example = "2026-04-10")
        val peakEndDate: LocalDate?,
        @field:Schema(description = "절정 지속일 (양 끝 포함)", example = "10")
        val peakDurationDays: Int?,
    )
}
