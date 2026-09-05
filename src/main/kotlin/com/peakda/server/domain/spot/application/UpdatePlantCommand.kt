package com.peakda.server.domain.spot.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Season

data class UpdatePlantCommand(
    val name: String? = null,
    val sortOrder: Int? = null,
    val status: PlantStatus? = null,
    val bloomCategory: BloomCategory? = null,
    val seasons: Set<Season>? = null,
)
