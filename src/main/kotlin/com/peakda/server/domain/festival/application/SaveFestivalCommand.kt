package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.FestivalKind
import java.time.LocalDate

data class SaveFestivalCommand(
    val kind: FestivalKind,
    val name: String,
    val venue: String,
    val startsOn: LocalDate,
    val endsOn: LocalDate?,
    val roadAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val homepageUrl: String?,
    val phoneNumber: String?,
    val hostOrganization: String?,
    val organizingInstitution: String?,
)
