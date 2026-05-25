package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import java.time.LocalDate

data class CreateSpotRecordCommand(
    val userId: Long,
    val spot: SpotResolveInput,
    val visitedDate: LocalDate?,
    val bloomStage: BloomStage?,
    val memo: String?,
    val plantIds: List<Long>,
    val photoKeys: List<String>,
    val status: SpotRecordStatus,
)
