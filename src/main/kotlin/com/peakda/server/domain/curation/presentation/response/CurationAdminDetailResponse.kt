package com.peakda.server.domain.curation.presentation.response

import com.peakda.server.domain.curation.entity.CurationLayout
import com.peakda.server.domain.curation.entity.CurationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate

@Schema(description = "백오피스 큐레이션 상세")
data class CurationAdminDetailResponse(
    @field:Schema(description = "큐레이션 id", example = "101")
    val id: Long,

    @field:Schema(description = "큐레이션 상태", example = "DRAFT")
    val status: CurationStatus,

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

    @field:Schema(description = "저장된 히어로 이미지 object key 또는 외부 URL", example = "curations/2026-07/a/main.webp")
    val heroImageKey: String?,

    @field:Schema(description = "히어로 이미지 미리보기 URL. 없으면 null")
    val heroImagePreviewUrl: String?,

    @field:Schema(description = "개행을 포함한 도입글. 없으면 null")
    val intro: String?,

    @field:Schema(description = "다음 주 예고 오버라인. 없으면 null", example = "다음 주 예고")
    val nextTeaserOverline: String?,

    @field:Schema(description = "다음 주 예고 본문. 없으면 null")
    val nextTeaserBody: String?,

    @field:Schema(description = "발행 시각. 임시저장이면 null")
    val publishedAt: Instant?,

    @field:Schema(description = "저장된 정렬 순서대로 반환한 큐레이션 챕터")
    val chapters: List<CurationAdminChapterResponse>,

    @field:Schema(description = "저장된 정렬 순서대로 반환한 당일치기 추천 카드")
    val recommendations: List<CurationAdminRecommendationResponse>,
) {
    @Schema(description = "백오피스 큐레이션 챕터")
    data class CurationAdminChapterResponse(
        @field:Schema(description = "챕터 표시 순서", example = "1")
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

        @field:Schema(description = "저장된 사진 object key 또는 외부 URL", example = "curations/2026-07/a/main.webp")
        val photoKey: String?,

        @field:Schema(description = "사진 미리보기 URL. 없으면 null")
        val photoPreviewUrl: String?,

        @field:Schema(description = "풀쿼트. 없으면 null")
        val pullQuote: String?,

        @field:Schema(description = "레이아웃별 리드 텍스트. 없으면 null")
        val leadText: String?,

        @field:Schema(description = "개행을 포함한 챕터 본문")
        val body: String,

        @field:Schema(description = "운영기간·입장료·주의사항을 보존한 단일 자유 텍스트. 없으면 null")
        val factNote: String?,
    )

    @Schema(description = "백오피스 큐레이션 당일치기 추천 카드")
    data class CurationAdminRecommendationResponse(
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

        @field:Schema(description = "저장된 사진 object key 또는 외부 URL", example = "curations/2026-07/b/main.webp")
        val photoKey: String?,

        @field:Schema(description = "사진 미리보기 URL. 없으면 null")
        val photoPreviewUrl: String?,

        @field:Schema(description = "추천 카드 설명")
        val body: String,
    )
}
