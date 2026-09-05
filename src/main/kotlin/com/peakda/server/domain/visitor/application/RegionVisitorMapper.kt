package com.peakda.server.domain.visitor.application

import com.peakda.server.domain.visitor.entity.RegionVisitor
import com.peakda.server.domain.visitor.repository.RegionVisitorUpsertCommand
import com.peakda.server.infrastructure.external.kto.datalab.response.MetcoVisitrItem

fun MetcoVisitrItem.toRegionVisitor(): RegionVisitor = RegionVisitor(
    baseDate = baseYmd,
    areaCode = areaCd,
    touristTypeCode = touDivCd,
    areaName = areaNm.ifBlank { null },
    touristTypeName = touDivNm.ifBlank { null },
    visitorCount = num.toLongOrNull(),
)

fun RegionVisitor.applyUpdate(item: MetcoVisitrItem) {
    areaName = item.areaNm.ifBlank { areaName }
    touristTypeName = item.touDivNm.ifBlank { touristTypeName }
    item.num.toLongOrNull()?.let { visitorCount = it }
}

fun MetcoVisitrItem.toUpsertCommand(): RegionVisitorUpsertCommand = RegionVisitorUpsertCommand(
    baseDate = baseYmd,
    areaCode = areaCd,
    touristTypeCode = touDivCd,
    areaName = areaNm.ifBlank { null },
    touristTypeName = touDivNm.ifBlank { null },
    visitorCount = num.toLongOrNull(),
)
