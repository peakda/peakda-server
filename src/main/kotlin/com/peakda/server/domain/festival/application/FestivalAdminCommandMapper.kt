package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.presentation.request.SaveFestivalRequest

fun SaveFestivalRequest.toCommand(): SaveFestivalCommand =
    SaveFestivalCommand(
        kind = kind,
        name = name.trim(),
        venue = venue.trim(),
        startsOn = startsOn,
        endsOn = endsOn,
        roadAddress = roadAddress.trimToNull(),
        latitude = latitude,
        longitude = longitude,
        homepageUrl = homepageUrl.trimToNull(),
        phoneNumber = phoneNumber.trimToNull(),
        hostOrganization = hostOrganization.trimToNull(),
        organizingInstitution = organizingInstitution.trimToNull(),
    )

private fun String?.trimToNull(): String? = this?.trim()?.ifEmpty { null }
