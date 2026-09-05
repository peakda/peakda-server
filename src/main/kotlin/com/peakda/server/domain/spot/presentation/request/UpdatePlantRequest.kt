package com.peakda.server.domain.spot.presentation.request

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.spot.application.UpdatePlantCommand
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Season
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "식물 검수 수정 요청")
data class UpdatePlantRequest(
    @field:NotBlank
    @field:Size(max = 30)
    @field:Schema(description = "정규화해 저장할 식물 이름", example = "왕벚나무", nullable = true)
    val name: String? = null,

    @field:Min(0)
    @field:Schema(description = "노출 정렬 순서", example = "10", nullable = true)
    val sortOrder: Int? = null,

    @field:Schema(description = "식물 상태", example = "ACTIVE", nullable = true)
    val status: PlantStatus? = null,

    @field:Schema(description = "개화 카테고리", example = "CHERRY", nullable = true)
    val bloomCategory: BloomCategory? = null,

    @field:Schema(
        description = "주개화 계절 전체 배열. 빈 배열이면 모두 제거한다.",
        example = "[\"SPRING\"]",
        nullable = true,
    )
    val seasons: Set<Season>? = null,
) {
    fun toCommand(): UpdatePlantCommand = UpdatePlantCommand(
        name = name,
        sortOrder = sortOrder,
        status = status,
        bloomCategory = bloomCategory,
        seasons = seasons,
    )
}
