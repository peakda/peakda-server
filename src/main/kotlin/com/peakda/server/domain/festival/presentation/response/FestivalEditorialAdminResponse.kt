package com.peakda.server.domain.festival.presentation.response

import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "관리자 축제 에디토리얼 편집 응답")
data class FestivalEditorialAdminResponse(
    @field:Schema(description = "축제 에디토리얼 id", example = "201")
    val editorialId: Long,

    @field:Schema(description = "축제 id", example = "101")
    val festivalId: Long,

    @field:Schema(description = "에디토리얼 훅. 없으면 null")
    val hook: String?,

    @field:Schema(description = "축제 기간 블록 서브텍스트. 없으면 null")
    val periodNote: String?,

    @field:Schema(description = "장소 블록 서브텍스트. 없으면 null")
    val placeNote: String?,

    @field:Schema(description = "입장료 블록 값. 없으면 null")
    val admissionFee: String?,

    @field:Schema(description = "입장료 블록 서브텍스트. 없으면 null")
    val admissionFeeNote: String?,

    @field:Schema(description = "운영 시간 블록 값. 없으면 null")
    val operatingHours: String?,

    @field:Schema(description = "운영 시간 블록 서브텍스트. 없으면 null")
    val operatingHoursNote: String?,

    @field:Schema(description = "주의사항 블록 값. 없으면 null")
    val caution: String?,

    @field:Schema(description = "주의사항 블록 서브텍스트. 없으면 null")
    val cautionNote: String?,

    @field:Schema(description = "개행으로 구분한 대중교통 안내. 없으면 null")
    val directionsTransit: String?,

    @field:Schema(description = "개행으로 구분한 자가 차량 안내. 없으면 null")
    val directionsCar: String?,

    @field:Schema(
        description = "DB에 저장된 원본 히어로 이미지 object key 또는 외부 URL. 저장 시 이 값을 다시 보낸다.",
        example = "curations/2026-07/550e8400-e29b-41d4-a716-446655440000/main.webp",
    )
    val heroImageKey: String?,

    @field:Schema(description = "미리보기용 resolved URL. 저장 payload에 넣지 않는다.")
    val heroImagePreviewUrl: String?,

    @field:Schema(description = "축제 에디토리얼 상태", example = "PUBLISHED")
    val status: FestivalEditorialStatus,

    @field:Schema(description = "발행 시각. DRAFT면 null")
    val publishedAt: Instant?,

    @field:Schema(description = "정렬된 주요 볼거리")
    val highlights: List<FestivalHighlightAdminResponse>,
) {
    @Schema(description = "관리자 축제 주요 볼거리 편집 응답")
    data class FestivalHighlightAdminResponse(
        @field:Schema(description = "표시 순서", example = "1")
        val sortOrder: Int,

        @field:Schema(description = "볼거리 타이틀", example = "꽃밭 트레킹 코스")
        val title: String,

        @field:Schema(description = "볼거리 설명", example = "백만 송이 꽃밭을 따라 천천히 걷는 코스예요.")
        val body: String,
    )
}
