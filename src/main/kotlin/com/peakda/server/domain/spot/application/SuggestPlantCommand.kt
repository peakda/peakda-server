package com.peakda.server.domain.spot.application

data class SuggestPlantCommand(
    val userId: Long,
    val name: String,
)
