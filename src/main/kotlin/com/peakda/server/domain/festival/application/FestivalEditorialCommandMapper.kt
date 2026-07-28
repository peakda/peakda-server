package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.presentation.request.UpsertFestivalEditorialRequest

fun UpsertFestivalEditorialRequest.toCommand(): UpsertFestivalEditorialCommand =
    UpsertFestivalEditorialCommand(
        hook = hook,
        periodNote = periodNote,
        placeNote = placeNote,
        admissionFee = admissionFee,
        admissionFeeNote = admissionFeeNote,
        operatingHours = operatingHours,
        operatingHoursNote = operatingHoursNote,
        caution = caution,
        cautionNote = cautionNote,
        directionsTransit = directionsTransit,
        directionsCar = directionsCar,
        heroImageKey = heroImageKey,
        status = status,
        highlights = highlights.map { highlight ->
            UpsertFestivalHighlightCommand(
                title = highlight.title,
                body = highlight.body,
            )
        },
    )
