package com.peakda.server.domain.festival.presentation.response

import com.peakda.server.domain.festival.application.FestivalPhase
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalKind
import com.peakda.server.domain.festival.entity.FestivalSource
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.Instant

@Schema(description = "관리자 축제 목록 요약")
data class FestivalAdminSummaryResponse(
    @field:Schema(description = "축제 id", example = "101")
    val id: Long,

    @field:Schema(description = "데이터 출처. PUBLIC_DATA(공공데이터)·MANUAL(직접 등록)", example = "PUBLIC_DATA")
    val source: FestivalSource,

    @field:Schema(description = "축제 종류. FESTIVAL(축제·행사)·TREE(트리 명소)", example = "FESTIVAL")
    val kind: FestivalKind,

    @field:Schema(description = "축제명", example = "태백 해바라기축제")
    val name: String,

    @field:Schema(description = "원천 행사 장소명", example = "구와우마을")
    val venue: String,

    @field:Schema(description = "정규화된 축제 시작일. 파싱할 수 없으면 null", example = "2026-07-18")
    val startsOn: LocalDate?,

    @field:Schema(description = "정규화된 축제 종료일. 없거나 파싱할 수 없으면 null", example = "2026-08-17")
    val endsOn: LocalDate?,

    @field:Schema(
        description = "오늘 기준 단계. UPCOMING(시작 전)·ONGOING(진행 중)·ENDING_SOON(종료 임박)·ENDED(종료). 시작일이 없으면 null",
        example = "ONGOING",
    )
    val phase: FestivalPhase?,

    @field:Schema(description = "에디토리얼 존재 여부", example = "true")
    val hasEditorial: Boolean,

    @field:Schema(description = "에디토리얼 상태. 에디토리얼이 없으면 null", example = "DRAFT")
    val editorialStatus: FestivalEditorialStatus?,

    @field:Schema(description = "에디토리얼 고정 시각. 고정하지 않았으면 null", example = "2026-10-27T01:00:00Z")
    val pinnedAt: Instant?,
)
