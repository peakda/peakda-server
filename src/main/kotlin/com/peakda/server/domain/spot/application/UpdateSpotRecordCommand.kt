package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.BloomStage
import java.time.LocalDate

data class UpdateSpotRecordCommand(
    val recordId: Long,
    val userId: Long,
    val visitedDate: LocalDate?,
    val bloomStage: BloomStage?,
    val memo: String?,
    val plantIds: List<Long>?,
    val photoKeys: List<String>?,
)
