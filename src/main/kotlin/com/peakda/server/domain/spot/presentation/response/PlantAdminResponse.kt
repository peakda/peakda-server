package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Season
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "관리자 식물 검수 응답")
data class PlantAdminResponse(
    @field:Schema(description = "식물 id", example = "4")
    val id: Long,
    @field:Schema(description = "식물 이름", example = "왕벚나무")
    val name: String,
    @field:Schema(description = "노출 정렬 순서", example = "10")
    val sortOrder: Int,
    @field:Schema(description = "식물 상태", example = "ACTIVE")
    val status: PlantStatus,
    @field:Schema(description = "제안한 사용자 id", example = "31", nullable = true)
    val suggestedByUserId: Long?,
    @field:Schema(description = "제안한 사용자 닉네임", example = "꽃길여행자", nullable = true)
    val suggestedByNickname: String?,
    @field:Schema(description = "승인 시각", example = "2026-07-28T09:30:00Z", nullable = true)
    val approvedAt: Instant?,
    @field:Schema(description = "개화 카테고리", example = "CHERRY", nullable = true)
    val bloomCategory: BloomCategory?,
    @field:Schema(description = "주개화 계절 전체", example = "[\"SPRING\"]")
    val seasons: List<Season>,
    @field:Schema(description = "등록 시각", example = "2026-07-28T09:20:00Z")
    val createdAt: Instant,
    @field:Schema(description = "수정 시각", example = "2026-07-28T09:30:00Z")
    val updatedAt: Instant,
) {
    companion object {
        fun from(plant: Plant, suggestedByNickname: String?): PlantAdminResponse = PlantAdminResponse(
            id = requireNotNull(plant.id),
            name = plant.name,
            sortOrder = plant.sortOrder,
            status = plant.status,
            suggestedByUserId = plant.suggestedByUserId,
            suggestedByNickname = suggestedByNickname,
            approvedAt = plant.approvedAt,
            bloomCategory = plant.bloomCategory,
            seasons = plant.seasons.sorted(),
            createdAt = plant.createdAt,
            updatedAt = plant.updatedAt,
        )
    }
}
