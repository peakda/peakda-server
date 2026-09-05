package com.peakda.server.domain.explore.presentation.response

import com.peakda.server.domain.curation.presentation.response.CurationCardResponse
import com.peakda.server.domain.festival.application.FestivalPhase
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "탐색 큐레이션(SCR-022) — 절정·피기 시작·진행 중 꽃축제·에디터 큐레이션")
data class ExploreResponse(
    @field:Schema(
        description = "개화 추정 산출 기준일. null이면 두 스팟 섹션은 빈 목록",
        example = "2026-04-01",
    )
    val baseDate: LocalDate?,

    @field:Schema(description = "탐색 기준일(KST)", example = "2026-04-02")
    val today: LocalDate,

    @field:Schema(description = "최신 산출일 기준 status=PEAK인 명소 카드", example = "[]")
    val peakNow: List<ExploreSpotItem>,

    @field:Schema(description = "최신 산출일 기준 status=STARTED인 명소 카드. 기본 5건", example = "[]")
    val nextWeek: List<ExploreSpotItem>,

    @field:Schema(description = "오늘 진행 중인 꽃축제를 종료 임박순으로 정렬한 카드", example = "[]")
    val festivals: List<ExploreFestivalItem>,

    @field:Schema(description = "발행된 에디터 큐레이션을 최신 주차순으로 정렬한 카드", example = "[]")
    val curations: List<CurationCardResponse>,
) {
    @Schema(description = "탐색 큐레이션 명소 카드")
    data class ExploreSpotItem(
        @field:Schema(
            description = "명소형 스팟 id. 정상 데이터에서는 항상 채워진다. null이면 좌표를 보유하지 않아 스팟을 생성할 수 없는 명소다",
            example = "100",
        )
        val spotId: Long?,

        @field:Schema(description = "명소 id", example = "501")
        val attractionId: Long,

        @field:Schema(description = "명소명", example = "여의도 한강공원")
        val name: String,

        @field:Schema(description = "명소 주소의 지역 표시값. 없으면 null", example = "서울 영등포구")
        val address: String?,

        @field:Schema(description = "대표 이미지 URL. 없으면 null", example = "https://img.peakda.kr/yeouido.jpg")
        val thumbnailUrl: String?,

        @field:Schema(description = "꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "꽃 카테고리 표시명", example = "벚꽃")
        val displayName: String,

        @field:Schema(description = "산출일 기준 개화 상태", example = "PEAK")
        val status: BloomStatus,

        @field:Schema(description = "추정 신뢰도", example = "0.92")
        val confidence: Double,

        @field:Schema(description = "만개 시작 예상일. 미정이면 null", example = "2026-04-03")
        val peakStartDate: LocalDate?,

        @field:Schema(description = "만개 종료 예상일. 미정이면 null", example = "2026-04-10")
        val peakEndDate: LocalDate?,

        @field:Schema(description = "현재 사용자가 찜한 스팟인지 여부. spotId가 null이면 항상 false", example = "true")
        val favorited: Boolean,

        @field:Schema(description = "찜 알림 활성화 여부. spotId가 null이면 항상 false", example = "true")
        val notifyEnabled: Boolean,

        @field:Schema(description = "명소 위도", example = "37.5284")
        val latitude: Double?,

        @field:Schema(description = "명소 경도", example = "126.9348")
        val longitude: Double?,
    )

    @Schema(description = "탐색 큐레이션 진행 중 꽃축제 카드")
    data class ExploreFestivalItem(
        @field:Schema(description = "축제 id", example = "701")
        val festivalId: Long,

        @field:Schema(description = "축제명", example = "진해 군항제")
        val name: String,

        @field:Schema(description = "개최 장소", example = "창원시 진해구 일원")
        val venue: String,

        @field:Schema(
            description = "주소 앞 두 토큰으로 만든 지역. 주소가 없으면 null",
            example = "경상남도 창원시",
        )
        val region: String?,

        @field:Schema(description = "축제 시작일(정규화 값)", example = "2026-04-01")
        val startsOn: LocalDate,

        @field:Schema(description = "축제 종료일(정규화 값). 없으면 null", example = "2026-04-10")
        val endsOn: LocalDate?,

        @field:Schema(description = "오늘부터 종료일까지 남은 일수. 종료일이 없으면 null", example = "5")
        val endsInDays: Long?,

        @field:Schema(description = "축제명으로 매칭된 꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "꽃 카테고리 표시명", example = "벚꽃")
        val displayName: String,

        @field:Schema(description = "축제 위도", example = "35.1594")
        val latitude: Double?,

        @field:Schema(description = "축제 경도", example = "128.6599")
        val longitude: Double?,

        @field:Schema(description = "축제 홈페이지. 없으면 null", example = "https://www.changwon.go.kr")
        val homepageUrl: String?,

        @field:Schema(description = "축제 카드 배경 이미지 URL. 발행 에디토리얼 hero 이미지가 우선된다", example = "https://img.peakda.kr/festivals/701/hero.jpg")
        val thumbnailUrl: String?,

        @field:Schema(description = "축제 상태", example = "ONGOING")
        val phase: FestivalPhase,
    )
}
