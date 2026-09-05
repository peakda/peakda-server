package com.peakda.server.domain.spot.presentation.request

import com.peakda.server.domain.spot.entity.SpotRecordStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관리자 스팟 기록 노출 상태 변경 요청")
data class UpdateSpotRecordStatusRequest(
    @field:Schema(description = "변경할 노출 상태 (PUBLISHED 또는 HIDDEN)", example = "HIDDEN")
    val status: SpotRecordStatus,
)
