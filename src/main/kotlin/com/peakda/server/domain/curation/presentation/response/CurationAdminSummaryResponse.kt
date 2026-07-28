package com.peakda.server.domain.curation.presentation.response

import com.peakda.server.domain.curation.entity.CurationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "백오피스 큐레이션 목록 요약")
data class CurationAdminSummaryResponse(
    @field:Schema(description = "큐레이션 id", example = "101")
    val id: Long,

    @field:Schema(description = "대상 주차 시작일", example = "2026-08-01")
    val weekStartDate: LocalDate,

    @field:Schema(description = "대상 주차 종료일", example = "2026-08-07")
    val weekEndDate: LocalDate,

    @field:Schema(description = "에디터가 작성한 주차 뱃지", example = "8월 1주차 · 8/1~8/7")
    val weekLabel: String,

    @field:Schema(description = "큐레이션 타이틀", example = "이번 주말, 노란색을 보러 가야 해요")
    val title: String,

    @field:Schema(description = "큐레이션 상태", example = "DRAFT")
    val status: CurationStatus,

    @field:Schema(description = "발행 시각. 임시저장이면 null")
    val publishedAt: Instant?,

    @field:Schema(description = "챕터 수", example = "3")
    val chapterCount: Long,

    @field:Schema(description = "추천 카드 수", example = "2")
    val recommendationCount: Long,
)
