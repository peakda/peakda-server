package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "찜한 스팟")
data class SpotFavoriteResponse(
    @field:Schema(description = "스팟 PK", example = "1024")
    val spotId: Long,

    @field:Schema(description = "스팟 분류", example = "ATTRACTION")
    val type: SpotType,

    @field:Schema(description = "스팟 표시명", example = "여의도 한강공원")
    val name: String,

    @field:Schema(description = "스팟 주소", example = "서울 영등포구 여의동로 330")
    val address: String?,

    @field:Schema(description = "ATTRACTION 일 때 attraction id", example = "501")
    val attractionId: Long?,

    @field:Schema(description = "만개 알림 수신 여부", example = "true")
    val notifyEnabled: Boolean,

    @field:Schema(description = "찜한 시각", example = "2026-05-29T09:41:00Z")
    val favoritedAt: Instant,

    @field:Schema(description = "대표 개화 정보. 명소형이 아니거나 추정이 없으면 null", example = "null")
    val bloom: Bloom?,

    @field:Schema(description = "이 스팟의 꽃 카테고리 칩. 명소형만 채워지고 그 외에는 빈 목록", example = "[]")
    val categories: List<CategoryChip>,

    @field:Schema(description = "게시된 방문 기록 수", example = "5")
    val recordCount: Long,

    @field:Schema(description = "카드 사진 URL. 최근 게시 기록 사진 최대 4장", example = "[]")
    val photoUrls: List<String>,
) {
    @Schema(description = "찜한 스팟의 대표 개화 정보")
    data class Bloom(
        @field:Schema(description = "대표 꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "대표 꽃 카테고리 표시명", example = "벚꽃")
        val displayName: String,

        @field:Schema(description = "대표 개화 상태", example = "PEAK")
        val status: BloomStatus,

        @field:Schema(description = "만개 시작일. 추정이 없으면 null", example = "2026-03-28")
        val peakStartDate: LocalDate?,

        @field:Schema(description = "만개 종료일. 추정이 없으면 null", example = "2026-04-05")
        val peakEndDate: LocalDate?,

        @field:Schema(description = "만개 지속일. 추정이 없으면 null", example = "9")
        val peakDurationDays: Int?,

        @field:Schema(description = "오늘부터 만개 시작일까지 남은 일수. 시작일이 없으면 null", example = "3")
        val daysUntilPeak: Long?,

        @field:Schema(description = "오늘 다음 날부터 설정된 임박 기간 안에 만개가 시작하는지", example = "true")
        val imminent: Boolean,

        @field:Schema(description = "개화 추정 산출 기준일", example = "2026-03-25")
        val baseDate: LocalDate,
    )

    @Schema(description = "꽃 카테고리 칩")
    data class CategoryChip(
        @field:Schema(description = "꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "꽃 카테고리 표시명", example = "벚꽃")
        val displayName: String,
    )
}
