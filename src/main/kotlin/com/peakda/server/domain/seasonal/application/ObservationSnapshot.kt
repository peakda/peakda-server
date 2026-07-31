package com.peakda.server.domain.seasonal.application

import java.time.LocalDate

data class ObservationSnapshot(
    val obsPlace: String,
    val floweringOn: LocalDate?,
    val fullBloomOn: LocalDate?,
)
