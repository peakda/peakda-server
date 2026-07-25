package com.peakda.server.domain.curation.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "큐레이션 등록·수정 결과")
data class CurationIdResponse(
    @field:Schema(description = "등록·수정된 큐레이션 id", example = "101")
    val curationId: Long,
)
