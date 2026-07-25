package com.peakda.server.domain.curation.presentation.request

import com.peakda.server.domain.curation.entity.CurationLayout
import com.peakda.server.domain.curation.entity.CurationStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "주차 기준 큐레이션 멱등 등록·수정 요청. 챕터·추천은 요청 배열 전체로 교체")
data class UpsertCurationRequest(
    @field:NotNull
    @field:Schema(description = "주차 시작일. 큐레이션 멱등 갱신 키", example = "2026-08-01")
    val weekStartDate: LocalDate,

    @field:NotNull
    @field:Schema(description = "주차 종료일", example = "2026-08-07")
    val weekEndDate: LocalDate,

    @field:NotBlank
    @field:Size(max = 100)
    @field:Schema(description = "에디터가 작성한 주차 뱃지", example = "8월 1주차 · 8/1~8/7")
    val weekLabel: String,

    @field:Size(max = 2000)
    @field:Schema(description = "히어로 이미지 URL. 없으면 null", example = "https://img.peakda.kr/curations/101/hero.jpg")
    val heroImageUrl: String? = null,

    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "큐레이션 타이틀", example = "이번 주말, 노란색을 보러 가야 해요")
    val title: String,

    @field:Size(max = 300)
    @field:Schema(description = "큐레이션 부제. 없으면 null", example = "해바라기가 가장 예쁜 세 곳")
    val subtitle: String? = null,

    @field:Size(max = 5000)
    @field:Schema(description = "개행을 포함한 최대 세 단락 도입글. 없으면 null")
    val intro: String? = null,

    @field:Size(max = 100)
    @field:Schema(description = "다음 주 예고 오버라인. 없으면 null", example = "다음 주 예고")
    val nextTeaserOverline: String? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "다음 주 예고 본문. 없으면 null")
    val nextTeaserBody: String? = null,

    @field:NotNull
    @field:Schema(description = "큐레이션 상태", example = "PUBLISHED")
    val status: CurationStatus,

    @field:Valid
    @field:Size(max = 3)
    @field:Schema(description = "요청 배열 순서대로 1부터 번호를 부여할 챕터")
    val chapters: List<Chapter> = emptyList(),

    @field:Valid
    @field:Schema(description = "요청 배열 순서대로 1부터 번호를 부여할 당일치기 추천 카드")
    val recommendations: List<Recommendation> = emptyList(),
) {
    @Schema(description = "큐레이션 챕터 입력")
    data class Chapter(
        @field:NotNull
        @field:Schema(description = "챕터 레이아웃", example = "MAIN")
        val layout: CurationLayout,

        @field:NotBlank
        @field:Size(max = 100)
        @field:Schema(description = "번호 뒤에 표시할 자유 레이블", example = "이번주 PEAKDA!")
        val heading: String,

        @field:Schema(description = "개화 뱃지·거리·상세 링크를 보강할 스팟 id. 없으면 null", example = "100")
        val spotId: Long? = null,

        @field:NotBlank
        @field:Size(max = 200)
        @field:Schema(description = "저장할 장소명", example = "태백 구와우마을")
        val placeName: String,

        @field:Schema(description = "저장할 장소 위도. 없으면 null", example = "37.1642")
        val latitude: Double? = null,

        @field:Schema(description = "저장할 장소 경도. 없으면 null", example = "128.9867")
        val longitude: Double? = null,

        @field:Size(max = 2000)
        @field:Schema(
            description = "챕터 사진 URL. 없으면 스팟 프리뷰 썸네일로 대체",
            example = "https://img.peakda.kr/a.jpg",
        )
        val photoUrl: String? = null,

        @field:Size(max = 500)
        @field:Schema(description = "풀쿼트. 없으면 null")
        val pullQuote: String? = null,

        @field:Size(max = 1000)
        @field:Schema(description = "레이아웃별 리드 텍스트. 없으면 null")
        val leadText: String? = null,

        @field:NotBlank
        @field:Size(max = 5000)
        @field:Schema(description = "개행을 포함한 최대 세 단락 본문")
        val body: String,

        @field:Size(max = 1000)
        @field:Schema(description = "운영기간·입장료·주의사항을 합친 단일 자유 텍스트. 없으면 null")
        val factNote: String? = null,
    )

    @Schema(description = "큐레이션 당일치기 추천 카드 입력")
    data class Recommendation(
        @field:NotBlank
        @field:Size(max = 200)
        @field:Schema(description = "추천 카드 타이틀", example = "노란색 당일치기")
        val title: String,

        @field:Schema(description = "거리·상세 링크를 보강할 스팟 id. 없으면 null", example = "100")
        val spotId: Long? = null,

        @field:NotBlank
        @field:Size(max = 200)
        @field:Schema(description = "저장할 장소명", example = "태백 구와우마을")
        val placeName: String,

        @field:Schema(description = "저장할 장소 위도. 없으면 null", example = "37.1642")
        val latitude: Double? = null,

        @field:Schema(description = "저장할 장소 경도. 없으면 null", example = "128.9867")
        val longitude: Double? = null,

        @field:Size(max = 2000)
        @field:Schema(
            description = "추천 사진 URL. 없으면 스팟 프리뷰 썸네일로 대체",
            example = "https://img.peakda.kr/b.jpg",
        )
        val photoUrl: String? = null,

        @field:NotBlank
        @field:Size(max = 3000)
        @field:Schema(description = "추천 카드 설명")
        val body: String,
    )
}
