package com.peakda.server.domain.festival.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "축제 등록 결과")
data class FestivalIdResponse(
    @field:Schema(description = "등록된 축제 id", example = "9001")
    val festivalId: Long,
)
