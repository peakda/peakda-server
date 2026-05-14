package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.entity.Congestion
import com.peakda.server.domain.congestion.repository.CongestionUpsertCommand
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem

fun CnctrRateItem.toCongestion(): Congestion = Congestion(
    baseDate = baseYmd,
    touristAttractionCode = tAtsCd,
    touristAttractionName = tAtsNm.ifBlank { null },
    areaCode = areaCd.ifBlank { null },
    sigunguCode = signguCd.ifBlank { null },
    congestionRate = cnctrRate.ifBlank { null },
)

fun Congestion.applyUpdate(item: CnctrRateItem) {
    touristAttractionName = item.tAtsNm.ifBlank { touristAttractionName }
    areaCode = item.areaCd.ifBlank { areaCode }
    sigunguCode = item.signguCd.ifBlank { sigunguCode }
    congestionRate = item.cnctrRate.ifBlank { congestionRate }
}

fun CnctrRateItem.toUpsertCommand(): CongestionUpsertCommand = CongestionUpsertCommand(
    baseDate = baseYmd,
    touristAttractionCode = tAtsCd,
    touristAttractionName = tAtsNm.ifBlank { null },
    areaCode = areaCd.ifBlank { null },
    sigunguCode = signguCd.ifBlank { null },
    congestionRate = cnctrRate.ifBlank { null },
)
