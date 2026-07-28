package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "찜한 스팟 목록(SCR-024c)")
data class SpotFavoriteListResponse(
    @field:Schema(description = "찜한 스팟 총 개수", example = "8")
    val count: Int,

    @field:Schema(description = "목록 상단 만개 배너. 알릴 개화가 없으면 null", example = "null")
    val banner: BloomBanner?,

    @field:Schema(description = "찜한 스팟 목록 (최근 찜한 순)", example = "[]")
    val favorites: List<SpotFavoriteResponse>,
) {
    @Schema(description = "목록 상단 만개 배너. 찜한 스팟 중 가장 먼저 알릴 1건")
    data class BloomBanner(
        @field:Schema(description = "스팟 PK", example = "1024")
        val spotId: Long,

        @field:Schema(description = "스팟 표시명", example = "진해 군항제")
        val spotName: String,

        @field:Schema(description = "대표 꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "대표 꽃 카테고리 표시명", example = "벚꽃")
        val displayName: String,

        @field:Schema(description = "대표 개화 상태", example = "PREPARING")
        val status: BloomStatus,

        @field:Schema(description = "만개 시작일", example = "2026-03-28")
        val peakStartDate: LocalDate,

        @field:Schema(description = "만개 종료일. 추정이 없으면 null", example = "2026-04-05")
        val peakEndDate: LocalDate?,

        @field:Schema(description = "오늘부터 만개 시작일까지 남은 일수", example = "3")
        val daysUntilPeak: Long,

        @field:Schema(description = "날짜를 제외한 배너 문구 본문", example = "진해 군항제가 곧 만개해요")
        val message: String,
    )
}
