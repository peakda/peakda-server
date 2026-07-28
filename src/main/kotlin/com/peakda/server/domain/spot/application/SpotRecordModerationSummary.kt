package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.SpotRecordStatus
import java.time.LocalDate

data class SpotRecordModerationSummary(
    val id: Long,
    val userId: Long,
    val status: SpotRecordStatus,
    val memo: String?,
    val visitedDate: LocalDate?,
)
