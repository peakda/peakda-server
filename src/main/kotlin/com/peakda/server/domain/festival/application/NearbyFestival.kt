package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.FestivalKind
import java.time.LocalDate

data class NearbyFestival(
    val festivalId: Long,
    val name: String,
    val kind: FestivalKind,
    val startsOn: LocalDate,
    val endsOn: LocalDate,
    val phase: FestivalPhase,
    val distanceMeters: Double,
)
