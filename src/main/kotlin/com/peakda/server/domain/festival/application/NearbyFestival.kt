package com.peakda.server.domain.festival.application

import java.time.LocalDate

data class NearbyFestival(
    val festivalId: Long,
    val name: String,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val phase: FestivalPhase,
    val distanceMeters: Double,
)
