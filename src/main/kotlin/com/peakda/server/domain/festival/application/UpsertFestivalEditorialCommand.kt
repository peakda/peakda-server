package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.FestivalEditorialStatus

data class UpsertFestivalEditorialCommand(
    val hook: String?,
    val periodNote: String?,
    val placeNote: String?,
    val admissionFee: String?,
    val admissionFeeNote: String?,
    val operatingHours: String?,
    val operatingHoursNote: String?,
    val caution: String?,
    val cautionNote: String?,
    val directionsTransit: String?,
    val directionsCar: String?,
    val heroImageUrl: String?,
    val status: FestivalEditorialStatus,
    val highlights: List<UpsertFestivalHighlightCommand>,
)
