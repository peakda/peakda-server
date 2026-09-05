package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "스팟 상세 응답 (지도 핀/검색 결과에서 진입하는 단일 스팟 화면)")
data class SpotDetailResponse(
    @field:Schema(description = "스팟 PK", example = "100")
    val id: Long,

    @field:Schema(description = "스팟 유형", example = "ATTRACTION")
    val type: SpotType,

    @field:Schema(description = "스팟명", example = "진해 여좌천")
    val name: String,

    @field:Schema(description = "주소", example = "경상남도 창원시 진해구 여좌동")
    val address: String?,

    @field:Schema(description = "위도", example = "35.1533")
    val latitude: Double,

    @field:Schema(description = "경도", example = "128.6712")
    val longitude: Double,

    @field:Schema(description = "연결된 명소 id (LOCAL 스팟이면 null)", example = "501")
    val attractionId: Long?,

    @field:Schema(
        description = "대표 사진 URL — ATTRACTION 은 명소 이미지, 없거나 LOCAL 이면 최근 방문 기록의 대표 사진. 둘 다 없으면 null",
    )
    val representativeImageUrl: String?,

    @field:Schema(description = "올해 만개 시기 배너 (개화 추정 연동). 추정 데이터가 없으면 null")
    val bloom: BloomBanner?,

    @field:Schema(description = "게시된 방문 기록 수", example = "12")
    val recordCount: Long,

    @field:Schema(description = "방문자 기록 프리뷰 (최신 게시순, 최대 3건)")
    val recordPreview: List<SpotRecordSummaryResponse>,

    @field:Schema(description = "현재 로그인 사용자의 찜 상태")
    val favorite: FavoriteState,
) {
    @Schema(description = "올해 만개 시기 배너 — 채택된 개화 추정 1건")
    data class BloomBanner(
        @field:Schema(description = "꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "카테고리 표시명", example = "벚꽃")
        val displayName: String,

        @field:Schema(description = "현재 개화 상태", example = "PEAK")
        val status: BloomStatus,

        @field:Schema(description = "신뢰도 (0~1)", example = "0.95")
        val confidence: Double,

        @field:Schema(description = "절정 시작일", example = "2026-03-28")
        val peakStartDate: LocalDate?,

        @field:Schema(description = "절정 종료일", example = "2026-04-05")
        val peakEndDate: LocalDate?,

        @field:Schema(description = "절정 지속일 (양 끝 포함)", example = "9")
        val peakDurationDays: Int?,

        @field:Schema(description = "상태 산출 기준일", example = "2026-03-30")
        val baseDate: LocalDate,
    )

    @Schema(description = "현재 사용자의 찜 상태")
    data class FavoriteState(
        @field:Schema(description = "찜 여부", example = "true")
        val favorited: Boolean,

        @field:Schema(description = "알림 활성화 여부 (찜하지 않았으면 false)", example = "true")
        val notifyEnabled: Boolean,
    )
}
