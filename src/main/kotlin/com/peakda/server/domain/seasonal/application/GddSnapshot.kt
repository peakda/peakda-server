package com.peakda.server.domain.seasonal.application

data class GddSnapshot(
    val stationId: String,
    val accumulated: Double,
)
