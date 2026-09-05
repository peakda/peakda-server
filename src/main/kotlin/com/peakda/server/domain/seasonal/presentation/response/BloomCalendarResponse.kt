package com.peakda.server.domain.seasonal.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "단일 명소×카테고리의 향후 예상 만개 캘린더 (온디맨드 시뮬레이션)")
data class BloomCalendarResponse(
    @field:Schema(description = "명소 id", example = "501")
    val attractionId: Long,

    @field:Schema(description = "꽃 카테고리", example = "CHERRY")
    val category: BloomCategory,

    @field:Schema(description = "카테고리 표시명", example = "벚꽃")
    val displayName: String,

    @field:Schema(description = "올해(인근 시즌) 절정 시작일", example = "2026-04-01")
    val peakStartDate: LocalDate?,

    @field:Schema(description = "절정 종료일", example = "2026-04-10")
    val peakEndDate: LocalDate?,

    @field:Schema(description = "절정 지속일 (양 끝 포함)", example = "10")
    val peakDurationDays: Int?,

    @field:Schema(description = "오늘부터의 일별 상태 타임라인")
    val days: List<BloomCalendarDay>,
) {
    @Schema(description = "특정 일자의 예상 상태")
    data class BloomCalendarDay(
        @field:Schema(description = "일자", example = "2026-06-06")
        val date: LocalDate,
        @field:Schema(description = "예상 상태", example = "PREPARING")
        val status: BloomStatus,
    )
}
