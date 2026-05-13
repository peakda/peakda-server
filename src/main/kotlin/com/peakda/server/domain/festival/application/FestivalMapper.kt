package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.infrastructure.external.pubdata.festival.response.FestivalItem

fun FestivalItem.toFestival(): Festival = Festival(
    fstvlNm = fstvlNm,
    opar = opar,
    fstvlStartDate = fstvlStartDate,
    fstvlEndDate = fstvlEndDate.ifBlank { null },
    mnnstNm = mnnstNm.ifBlank { null },
    auspcInsttNm = auspcInsttNm.ifBlank { null },
    suprtInsttNm = suprtInsttNm.ifBlank { null },
    phoneNumber = phoneNumber.ifBlank { null },
    homepageUrl = homepageUrl.ifBlank { null },
    rdnmadr = rdnmadr.ifBlank { null },
    lnmadr = lnmadr.ifBlank { null },
    latitude = latitude.toDoubleOrNull(),
    longitude = longitude.toDoubleOrNull(),
    referenceDate = referenceDate.ifBlank { null },
    insttCode = insttCode.ifBlank { null },
    insttNm = insttNm.ifBlank { null },
)

fun Festival.applyUpdate(item: FestivalItem) {
    fstvlEndDate = item.fstvlEndDate.ifBlank { fstvlEndDate }
    mnnstNm = item.mnnstNm.ifBlank { mnnstNm }
    auspcInsttNm = item.auspcInsttNm.ifBlank { auspcInsttNm }
    suprtInsttNm = item.suprtInsttNm.ifBlank { suprtInsttNm }
    phoneNumber = item.phoneNumber.ifBlank { phoneNumber }
    homepageUrl = item.homepageUrl.ifBlank { homepageUrl }
    rdnmadr = item.rdnmadr.ifBlank { rdnmadr }
    lnmadr = item.lnmadr.ifBlank { lnmadr }
    item.latitude.toDoubleOrNull()?.let { latitude = it }
    item.longitude.toDoubleOrNull()?.let { longitude = it }
    referenceDate = item.referenceDate.ifBlank { referenceDate }
    insttCode = item.insttCode.ifBlank { insttCode }
    insttNm = item.insttNm.ifBlank { insttNm }
}
