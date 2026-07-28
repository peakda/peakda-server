package com.peakda.server.domain.festival.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "축제 에디토리얼 등록·수정 결과")
data class FestivalEditorialIdResponse(
    @field:Schema(description = "등록·수정된 축제 에디토리얼 id", example = "201")
    val editorialId: Long,
)
