package com.peakda.server.domain.festival.presentation.response

import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "관리자 축제 목록 요약")
data class FestivalAdminSummaryResponse(
    @field:Schema(description = "축제 id", example = "101")
    val id: Long,

    @field:Schema(description = "축제명", example = "태백 해바라기축제")
    val name: String,

    @field:Schema(description = "원천 행사 장소명", example = "구와우마을")
    val venue: String,

    @field:Schema(description = "정규화된 축제 시작일. 파싱할 수 없으면 null", example = "2026-07-18")
    val startsOn: LocalDate?,

    @field:Schema(description = "정규화된 축제 종료일. 없거나 파싱할 수 없으면 null", example = "2026-08-17")
    val endsOn: LocalDate?,

    @field:Schema(description = "에디토리얼 존재 여부", example = "true")
    val hasEditorial: Boolean,

    @field:Schema(description = "에디토리얼 상태. 에디토리얼이 없으면 null", example = "DRAFT")
    val editorialStatus: FestivalEditorialStatus?,
)
