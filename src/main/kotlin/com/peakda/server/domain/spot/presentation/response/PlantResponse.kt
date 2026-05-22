package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.spot.entity.PlantStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "식물 응답 — 마스터 칩, 검색 결과, 제안 응답 공용")
data class PlantResponse(
    @field:Schema(description = "식물 PK", example = "4")
    val id: Long,
    @field:Schema(description = "식물 이름", example = "벚꽃")
    val name: String,
    @field:Schema(description = "식물 상태 (ACTIVE 노출, PENDING 제안 검토 중)", example = "ACTIVE")
    val status: PlantStatus,
)
