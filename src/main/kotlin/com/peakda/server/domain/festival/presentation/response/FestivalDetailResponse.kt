package com.peakda.server.domain.festival.presentation.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.peakda.server.domain.festival.application.FestivalPhase
import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "축제 상세")
data class FestivalDetailResponse(
    @field:Schema(description = "축제 id", example = "101")
    val festivalId: Long,

    @field:Schema(description = "축제명", example = "태백 해바라기축제")
    val name: String,

    @field:Schema(description = "원천 행사 장소명", example = "구와우마을")
    val venue: String,

    @field:Schema(description = "원천 도로명 주소. 없으면 null", example = "강원특별자치도 태백시 구와우길 38-20")
    val roadAddress: String?,

    @field:Schema(description = "지도 CTA에 사용할 위도. 없으면 null", example = "37.1642")
    val latitude: Double?,

    @field:Schema(description = "지도 CTA에 사용할 경도. 없으면 null", example = "128.9867")
    val longitude: Double?,

    @field:Schema(description = "축제 홈페이지 URL. 없으면 null", example = "https://example.com/festival")
    val homepageUrl: String?,

    @field:Schema(description = "축제명으로 판정한 꽃 카테고리. 매칭되지 않으면 null", example = "CHERRY")
    val category: BloomCategory?,

    @field:Schema(description = "꽃 카테고리 표시명. 카테고리가 없으면 null", example = "벚꽃")
    val displayName: String?,

    @field:Schema(description = "정규화된 축제 시작일. 파싱할 수 없으면 null", example = "2026-07-18")
    val startsOn: LocalDate?,

    @field:Schema(description = "정규화된 축제 종료일. 없거나 파싱할 수 없으면 null", example = "2026-08-17")
    val endsOn: LocalDate?,

    @field:Schema(
        description = "시작일부터 종료일까지 양 끝 날짜를 포함한 기간 일수. 판정할 수 없으면 null",
        example = "31",
    )
    val durationDays: Int?,

    @field:Schema(description = "서버가 판정한 축제 상태. 시작일을 판정할 수 없으면 null", example = "ENDING_SOON")
    val phase: FestivalPhase?,

    @get:JsonProperty("dDay")
    @field:Schema(description = "UPCOMING일 때 시작까지 남은 일수. 그 외에는 null", example = "12")
    val dDay: Long?,

    @field:Schema(description = "진행 중일 때 종료까지 남은 일수. 그 외에는 null", example = "5")
    val endsInDays: Long?,

    @field:Schema(
        description = "발행된 에디토리얼. 없거나 DRAFT이면 null",
        example = "null",
    )
    val editorial: FestivalEditorialResponse?,
) {
    @Schema(description = "사람이 작성한 축제 운영·에디토리얼 정보")
    data class FestivalEditorialResponse(
        @field:Schema(
            description = "에디토리얼 훅. 없으면 null",
            example = "국내 최대 규모의 해바라기 축제예요. 해발 800m 고산지대에서 백만 송이가 피어나요.",
        )
        val hook: String?,

        @field:Schema(description = "히어로 이미지 URL. 없으면 null", example = "https://img.peakda.kr/festivals/101/hero.jpg")
        val heroImageUrl: String?,

        @field:Schema(
            description = "축제 기간 블록 서브텍스트. 없으면 null",
            example = "2026년 일정은 출발 전 재확인",
        )
        val periodNote: String?,

        @field:Schema(
            description = "장소 블록 서브텍스트. 없으면 null",
            example = "해발 800m · 서울에서 약 2시간 40분",
        )
        val placeNote: String?,

        @field:Schema(description = "입장료 블록 값. 없으면 null", example = "성인 7,000원 · 청소년 5,000원")
        val admissionFee: String?,

        @field:Schema(description = "입장료 블록 서브텍스트. 없으면 null", example = "어린이(초등 이하) 3,000원")
        val admissionFeeNote: String?,

        @field:Schema(description = "운영 시간 블록 값. 없으면 null", example = "09:00 ~ 18:00")
        val operatingHours: String?,

        @field:Schema(description = "운영 시간 블록 서브텍스트. 없으면 null", example = "입장 마감 17:30")
        val operatingHoursNote: String?,

        @field:Schema(
            description = "주의사항 블록 값. 없으면 화면에서 블록을 제외",
            example = "고산지대 날씨 급변 · 겉옷 필수",
        )
        val caution: String?,

        @field:Schema(description = "주의사항 블록 서브텍스트. 없으면 null", example = "오전 방문 권장 (오후 역광)")
        val cautionNote: String?,

        @field:Schema(
            description = "개행으로 구분한 대중교통 안내. 없으면 null",
            example = "서울 → KTX 태백역 (약 2시간) → 택시 15분\\n동서울터미널 → 태백 직행버스 (약 3시간)",
        )
        val directionsTransit: String?,

        @field:Schema(
            description = "개행으로 구분한 자가 차량 안내. 없으면 null",
            example = "서울 → 영동고속도로 → 태백\\n축제장 임시 주차장 이용",
        )
        val directionsCar: String?,

        @field:Schema(
            description = "정렬된 주요 볼거리",
            example = """[{"sortOrder":1,"title":"꽃밭 트레킹 코스","body":"백만 송이 꽃밭을 걷는 코스예요."}]""",
        )
        val highlights: List<FestivalHighlightResponse>,
    )

    @Schema(description = "축제 주요 볼거리")
    data class FestivalHighlightResponse(
        @field:Schema(description = "표시 순서", example = "1")
        val sortOrder: Int,

        @field:Schema(description = "볼거리 타이틀", example = "꽃밭 트레킹 코스")
        val title: String,

        @field:Schema(description = "볼거리 설명", example = "백만 송이 꽃밭을 따라 천천히 걷는 코스예요.")
        val body: String,
    )
}
