package com.peakda.server.domain.user.presentation.request

import com.peakda.server.domain.seasonal.entity.BloomCategory
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "관심 꽃 카테고리 수정 요청 (전체 교체)")
data class FavoriteCategoryUpdateRequest(
    @field:Schema(
        description = "관심 꽃 카테고리 목록. 최소 1개 이상 선택해야 한다.",
        example = "[\"CHERRY\", \"MAPLE\"]",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotEmpty
    val categories: Set<BloomCategory>,
)
