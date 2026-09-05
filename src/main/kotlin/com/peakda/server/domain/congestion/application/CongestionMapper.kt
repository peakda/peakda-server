package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.repository.CongestionUpsertCommand
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem

fun CnctrRateItem.toUpsertCommand(): CongestionUpsertCommand = CongestionUpsertCommand(
    baseDate = baseYmd,
    areaCode = areaCd,
    sigunguCode = signguCd,
    touristAttractionName = tAtsNm,
    congestionRate = cnctrRate.ifBlank { null },
)
