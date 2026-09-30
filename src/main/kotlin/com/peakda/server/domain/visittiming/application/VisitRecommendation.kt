package com.peakda.server.domain.visittiming.application

import java.time.LocalDate

data class VisitRecommendation(
    val date: LocalDate,
    val reasons: List<VisitReason>,
)
