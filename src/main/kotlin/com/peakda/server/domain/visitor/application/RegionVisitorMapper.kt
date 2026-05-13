package com.peakda.server.domain.visitor.application

import com.peakda.server.domain.visitor.entity.RegionVisitor
import com.peakda.server.infrastructure.external.kto.datalab.response.MetcoVisitrItem

fun MetcoVisitrItem.toRegionVisitor(): RegionVisitor = RegionVisitor(
    baseYmd = baseYmd,
    areaCd = areaCd,
    touDivCd = touDivCd,
    areaNm = areaNm.ifBlank { null },
    touDivNm = touDivNm.ifBlank { null },
    num = num.toLongOrNull(),
)

fun RegionVisitor.applyUpdate(item: MetcoVisitrItem) {
    areaNm = item.areaNm.ifBlank { areaNm }
    touDivNm = item.touDivNm.ifBlank { touDivNm }
    item.num.toLongOrNull()?.let { num = it }
}
