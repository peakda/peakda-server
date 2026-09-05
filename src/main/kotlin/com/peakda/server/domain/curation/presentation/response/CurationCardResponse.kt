package com.peakda.server.domain.curation.presentation.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "탐색 카드와 큐레이션 목록에서 공용으로 사용하는 발행 큐레이션 요약")
data class CurationCardResponse(
    @field:Schema(description = "큐레이션 id", example = "101")
    val id: Long,

    @field:Schema(description = "에디터가 작성한 주차 뱃지", example = "8월 1주차 · 8/1~8/7")
    val weekLabel: String,

    @field:Schema(description = "대상 주차 시작일", example = "2026-08-01")
    val weekStartDate: LocalDate,

    @field:Schema(description = "대상 주차 종료일", example = "2026-08-07")
    val weekEndDate: LocalDate,

    @field:Schema(description = "큐레이션 타이틀", example = "이번 주말, 노란색을 보러 가야 해요")
    val title: String,

    @field:Schema(description = "큐레이션 부제. 없으면 null", example = "해바라기가 가장 예쁜 세 곳")
    val subtitle: String?,

    @field:Schema(description = "히어로 이미지 URL. 없으면 null", example = "https://img.peakda.kr/curations/101/hero.jpg")
    val heroImageUrl: String?,
)
