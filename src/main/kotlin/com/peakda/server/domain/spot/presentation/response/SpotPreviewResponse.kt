package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.spot.entity.SpotType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "핀 클릭 프리뷰 카드 목록 (단일 핀=SCR-011e, 클러스터=SCR-011d 양쪽에서 사용)")
data class SpotPreviewResponse(
    @field:Schema(description = "요청한 spotIds 순서를 보존한 프리뷰 카드 목록 (존재하지 않거나 비공개인 id는 제외)")
    val items: List<SpotPreviewItem>,
) {
    @Schema(description = "핀 프리뷰 카드 1건")
    data class SpotPreviewItem(
        @field:Schema(description = "스팟 id", example = "100")
        val spotId: Long,

        @field:Schema(description = "핀 유형", example = "ATTRACTION")
        val type: SpotType,

        @field:Schema(description = "핀 이름", example = "남산")
        val name: String,

        @field:Schema(description = "썸네일 이미지 URL (없으면 null)", example = "https://img/thumb.jpg")
        val thumbnailUrl: String?,

        @field:Schema(description = "대표 개화 단계 뱃지 (개화 신호가 없으면 null)")
        val badge: BloomBadge?,

        @field:Schema(description = "요청 좌표(lat/lng)로부터의 거리(m). 좌표 미전달 시 null", example = "320.5")
        val distanceMeters: Double?,
    )

    @Schema(description = "대표 개화 단계 뱃지")
    data class BloomBadge(
        @field:Schema(description = "꽃 카테고리", example = "CHERRY")
        val category: BloomCategory,

        @field:Schema(description = "카테고리 표시명", example = "벚꽃")
        val displayName: String,

        @field:Schema(description = "현재 상태 (PREPARING/STARTED/PEAK)", example = "PEAK")
        val status: BloomStatus,
    )
}
