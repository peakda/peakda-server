package com.peakda.server.domain.curation.presentation.response

import com.peakda.server.domain.curation.entity.CurationLayout
import com.peakda.server.domain.spot.presentation.response.SpotPreviewResponse.BloomBadge
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "주차 단위 에디토리얼 큐레이션 상세(SCR-026)")
data class CurationDetailResponse(
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

    @field:Schema(
        description = "개행을 포함한 도입글. 없으면 null",
        example = "8월의 첫 주말, 우리는 노란색을 보러 가야 해요.",
    )
    val intro: String?,

    @field:Schema(description = "다음 주 예고 오버라인. 없으면 null", example = "다음 주 예고")
    val nextTeaserOverline: String?,

    @field:Schema(
        description = "다음 주 예고 본문. 없으면 null",
        example = "해바라기 만개 피크, 지금이 제일 예쁩니다.",
    )
    val nextTeaserBody: String?,

    @field:Schema(description = "정렬 순서대로 조립한 큐레이션 챕터")
    val chapters: List<CurationChapterResponse>,

    @field:Schema(description = "정렬 순서대로 조립한 당일치기 추천 카드")
    val recommendations: List<CurationRecommendationResponse>,
) {
    @Schema(description = "큐레이션 장소 챕터")
    data class CurationChapterResponse(
        @field:Schema(description = "챕터 표시 순서. 프론트가 01, 02 형태로 렌더링", example = "1")
        val sortOrder: Int,

        @field:Schema(description = "챕터 레이아웃", example = "MAIN")
        val layout: CurationLayout,

        @field:Schema(description = "번호 뒤에 표시할 자유 레이블", example = "이번주 PEAKDA!")
        val heading: String,

        @field:Schema(description = "연결된 스팟 id. 연결하지 않았으면 null", example = "100")
        val spotId: Long?,

        @field:Schema(description = "에디터가 작성한 장소명", example = "태백 구와우마을")
        val placeName: String,

        @field:Schema(description = "저장된 장소 위도. 없으면 null", example = "37.1642")
        val latitude: Double?,

        @field:Schema(description = "저장된 장소 경도. 없으면 null", example = "128.9867")
        val longitude: Double?,

        @field:Schema(
            description = "챕터 사진 URL. 저장값이 없으면 스팟 프리뷰 썸네일로 대체",
            example = "https://img.peakda.kr/a.jpg",
        )
        val photoUrl: String?,

        @field:Schema(description = "풀쿼트. 없으면 null", example = "노란 파도가 산 아래까지 이어져요.")
        val pullQuote: String?,

        @field:Schema(description = "레이아웃별 리드 텍스트. 없으면 null", example = "오늘 당장 떠나도 좋아요.")
        val leadText: String?,

        @field:Schema(
            description = "개행을 포함한 챕터 본문",
            example = "아침 일찍 도착하면 한적하게 걸을 수 있어요.",
        )
        val body: String,

        @field:Schema(
            description = "운영기간·입장료·주의사항을 보존한 단일 자유 텍스트. 없으면 null",
            example = "📅 8월 말까지 운영 · 입장료 성인 15,000원 (시기별 변동)",
        )
        val factNote: String?,

        @field:Schema(description = "연결 스팟의 대표 개화 뱃지. 연결 정보가 없으면 null")
        val badge: BloomBadge?,

        @field:Schema(
            description = "요청 좌표로부터 연결 스팟까지의 거리(m). 좌표나 연결 정보가 없으면 null",
            example = "320.5",
        )
        val distanceMeters: Double?,
    )

    @Schema(description = "큐레이션 당일치기 추천 카드")
    data class CurationRecommendationResponse(
        @field:Schema(description = "추천 카드 표시 순서", example = "1")
        val sortOrder: Int,

        @field:Schema(description = "추천 카드 타이틀", example = "노란색 당일치기")
        val title: String,

        @field:Schema(description = "연결된 스팟 id. 연결하지 않았으면 null", example = "100")
        val spotId: Long?,

        @field:Schema(description = "에디터가 작성한 장소명", example = "태백 구와우마을")
        val placeName: String,

        @field:Schema(description = "저장된 장소 위도. 없으면 null", example = "37.1642")
        val latitude: Double?,

        @field:Schema(description = "저장된 장소 경도. 없으면 null", example = "128.9867")
        val longitude: Double?,

        @field:Schema(
            description = "추천 사진 URL. 저장값이 없으면 스팟 프리뷰 썸네일로 대체",
            example = "https://img.peakda.kr/b.jpg",
        )
        val photoUrl: String?,

        @field:Schema(description = "추천 카드 설명", example = "아침에 출발해 해 질 무렵 돌아오는 코스예요.")
        val body: String,

        @field:Schema(
            description = "요청 좌표로부터 연결 스팟까지의 거리(m). 좌표나 연결 정보가 없으면 null",
            example = "320.5",
        )
        val distanceMeters: Double?,
    )
}
