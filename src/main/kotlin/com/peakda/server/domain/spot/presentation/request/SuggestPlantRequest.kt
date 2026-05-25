package com.peakda.server.domain.spot.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "검색에서 찾지 못한 식물을 사용자가 추가 제안")
data class SuggestPlantRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 30)
    @field:Schema(description = "제안할 식물 이름 (공백 정규화 후 저장)", example = "구절초")
    val name: String,
)
