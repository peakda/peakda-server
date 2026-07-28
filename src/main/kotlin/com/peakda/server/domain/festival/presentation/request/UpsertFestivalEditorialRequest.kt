package com.peakda.server.domain.festival.presentation.request

import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "축제 기준 에디토리얼 멱등 등록·수정 요청. 주요 볼거리는 요청 배열 전체로 교체")
data class UpsertFestivalEditorialRequest(
    @field:Size(max = 5000)
    @field:Schema(
        description = "에디토리얼 훅. 없으면 null",
        example = "국내 최대 규모의 해바라기 축제예요. 해발 800m 고산지대에서 백만 송이가 피어나요.",
    )
    val hook: String? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "축제 기간 블록 서브텍스트. 없으면 null", example = "2026년 일정은 출발 전 재확인")
    val periodNote: String? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "장소 블록 서브텍스트. 없으면 null", example = "해발 800m · 서울에서 약 2시간 40분")
    val placeNote: String? = null,

    @field:Size(max = 1000)
    @field:Schema(
        description = "입장료 블록 값. 무료를 포함하며 없으면 null",
        example = "성인 7,000원 · 청소년 5,000원",
    )
    val admissionFee: String? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "입장료 블록 서브텍스트. 없으면 null", example = "어린이(초등 이하) 3,000원")
    val admissionFeeNote: String? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "운영 시간 블록 값. 없으면 null", example = "09:00 ~ 18:00")
    val operatingHours: String? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "운영 시간 블록 서브텍스트. 없으면 null", example = "입장 마감 17:30")
    val operatingHoursNote: String? = null,

    @field:Size(max = 1000)
    @field:Schema(
        description = "주의사항 블록 값. 없으면 화면에서 블록을 제외",
        example = "고산지대 날씨 급변 · 겉옷 필수",
    )
    val caution: String? = null,

    @field:Size(max = 1000)
    @field:Schema(description = "주의사항 블록 서브텍스트. 없으면 null", example = "오전 방문 권장 (오후 역광)")
    val cautionNote: String? = null,

    @field:Size(max = 5000)
    @field:Schema(
        description = "개행으로 구분한 대중교통 안내. 없으면 null",
        example = "서울 → KTX 태백역 (약 2시간) → 택시 15분\\n동서울터미널 → 태백 직행버스 (약 3시간)",
    )
    val directionsTransit: String? = null,

    @field:Size(max = 5000)
    @field:Schema(
        description = "개행으로 구분한 자가 차량 안내. 없으면 null",
        example = "서울 → 영동고속도로 → 태백\\n축제장 임시 주차장 이용",
    )
    val directionsCar: String? = null,

    @field:Size(max = 2000)
    @field:Schema(description = "히어로 이미지 URL. 없으면 null", example = "https://img.peakda.kr/festivals/101/hero.jpg")
    val heroImageUrl: String? = null,

    @field:NotNull
    @field:Schema(description = "축제 에디토리얼 상태", example = "PUBLISHED")
    val status: FestivalEditorialStatus,

    @field:Valid
    @field:Size(max = 3)
    @field:Schema(
        description = "요청 배열 순서대로 1부터 번호를 부여할 주요 볼거리. 최대 3건",
        example = """[{"title":"꽃밭 트레킹 코스","body":"백만 송이 꽃밭을 따라 천천히 걷는 코스예요."}]""",
    )
    val highlights: List<Highlight> = emptyList(),
) {
    @Schema(description = "축제 주요 볼거리 입력")
    data class Highlight(
        @field:NotBlank
        @field:Size(max = 200)
        @field:Schema(description = "볼거리 타이틀", example = "꽃밭 트레킹 코스")
        val title: String,

        @field:NotBlank
        @field:Size(max = 3000)
        @field:Schema(description = "볼거리 설명", example = "백만 송이 꽃밭을 따라 천천히 걷는 코스예요.")
        val body: String,
    )
}
