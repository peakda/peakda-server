package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.repository.FestivalUpsertCommand
import com.peakda.server.infrastructure.external.pubdata.festival.response.FestivalItem

fun FestivalItem.toFestival(): Festival = Festival(
    name = fstvlNm,
    venue = opar,
    startDate = fstvlStartDate,
    endDate = fstvlEndDate.ifBlank { null },
    startsOn = FestivalDates.parse(fstvlStartDate),
    endsOn = FestivalDates.parse(fstvlEndDate),
    hostOrganization = mnnstNm.ifBlank { null },
    organizingInstitution = auspcInsttNm.ifBlank { null },
    supportingInstitution = suprtInsttNm.ifBlank { null },
    phoneNumber = phoneNumber.ifBlank { null },
    homepageUrl = homepageUrl.ifBlank { null },
    roadAddress = rdnmadr.ifBlank { null },
    landLotAddress = lnmadr.ifBlank { null },
    latitude = latitude.toDoubleOrNull(),
    longitude = longitude.toDoubleOrNull(),
    referenceDate = referenceDate.ifBlank { null },
    providerInstitutionCode = insttCode.ifBlank { null },
    providerInstitutionName = insttNm.ifBlank { null },
)

fun Festival.applyUpdate(item: FestivalItem) {
    endDate = item.fstvlEndDate.ifBlank { endDate }
    endsOn = FestivalDates.parse(endDate)
    startsOn = startsOn ?: FestivalDates.parse(startDate)
    hostOrganization = item.mnnstNm.ifBlank { hostOrganization }
    organizingInstitution = item.auspcInsttNm.ifBlank { organizingInstitution }
    supportingInstitution = item.suprtInsttNm.ifBlank { supportingInstitution }
    phoneNumber = item.phoneNumber.ifBlank { phoneNumber }
    homepageUrl = item.homepageUrl.ifBlank { homepageUrl }
    roadAddress = item.rdnmadr.ifBlank { roadAddress }
    landLotAddress = item.lnmadr.ifBlank { landLotAddress }
    item.latitude.toDoubleOrNull()?.let { latitude = it }
    item.longitude.toDoubleOrNull()?.let { longitude = it }
    referenceDate = item.referenceDate.ifBlank { referenceDate }
    providerInstitutionCode = item.insttCode.ifBlank { providerInstitutionCode }
    providerInstitutionName = item.insttNm.ifBlank { providerInstitutionName }
}

fun FestivalItem.toUpsertCommand(): FestivalUpsertCommand = FestivalUpsertCommand(
    name = fstvlNm,
    venue = opar,
    startDate = fstvlStartDate,
    endDate = fstvlEndDate.ifBlank { null },
    startsOn = FestivalDates.parse(fstvlStartDate),
    endsOn = FestivalDates.parse(fstvlEndDate),
    hostOrganization = mnnstNm.ifBlank { null },
    organizingInstitution = auspcInsttNm.ifBlank { null },
    supportingInstitution = suprtInsttNm.ifBlank { null },
    phoneNumber = phoneNumber.ifBlank { null },
    homepageUrl = homepageUrl.ifBlank { null },
    roadAddress = rdnmadr.ifBlank { null },
    landLotAddress = lnmadr.ifBlank { null },
    latitude = latitude.toDoubleOrNull(),
    longitude = longitude.toDoubleOrNull(),
    referenceDate = referenceDate.ifBlank { null },
    providerInstitutionCode = insttCode.ifBlank { null },
    providerInstitutionName = insttNm.ifBlank { null },
)
