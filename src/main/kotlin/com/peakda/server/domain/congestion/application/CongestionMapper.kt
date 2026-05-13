package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.congestion.entity.Congestion
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem

fun CnctrRateItem.toCongestion(): Congestion = Congestion(
    baseYmd = baseYmd,
    tAtsCd = tAtsCd,
    tAtsNm = tAtsNm.ifBlank { null },
    areaCd = areaCd.ifBlank { null },
    signguCd = signguCd.ifBlank { null },
    cnctrRate = cnctrRate.ifBlank { null },
)

fun Congestion.applyUpdate(item: CnctrRateItem) {
    tAtsNm = item.tAtsNm.ifBlank { tAtsNm }
    areaCd = item.areaCd.ifBlank { areaCd }
    signguCd = item.signguCd.ifBlank { signguCd }
    cnctrRate = item.cnctrRate.ifBlank { cnctrRate }
}
