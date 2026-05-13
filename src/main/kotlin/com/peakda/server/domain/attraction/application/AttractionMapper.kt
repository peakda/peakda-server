package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedSyncListItem

fun AreaBasedSyncListItem.toAttraction(): Attraction = Attraction(
    contentId = contentid,
    contentTypeId = contenttypeid.ifBlank { null },
    title = title,
    addr1 = addr1.ifBlank { null },
    addr2 = addr2.ifBlank { null },
    areaCode = areacode.ifBlank { null },
    sigunguCode = sigungucode.ifBlank { null },
    mapX = mapx.toDoubleOrNull(),
    mapY = mapy.toDoubleOrNull(),
    firstImage = firstimage.ifBlank { null },
    firstImage2 = firstimage2.ifBlank { null },
    cat1 = cat1.ifBlank { null },
    cat2 = cat2.ifBlank { null },
    cat3 = cat3.ifBlank { null },
    createdTime = createdtime.ifBlank { null },
    modifiedTime = modifiedtime.ifBlank { null },
    visible = showflag != "0",
)

fun Attraction.applyUpdate(item: AreaBasedSyncListItem) {
    contentTypeId = item.contenttypeid.ifBlank { contentTypeId }
    title = item.title
    addr1 = item.addr1.ifBlank { addr1 }
    addr2 = item.addr2.ifBlank { addr2 }
    areaCode = item.areacode.ifBlank { areaCode }
    sigunguCode = item.sigungucode.ifBlank { sigunguCode }
    item.mapx.toDoubleOrNull()?.let { mapX = it }
    item.mapy.toDoubleOrNull()?.let { mapY = it }
    firstImage = item.firstimage.ifBlank { firstImage }
    firstImage2 = item.firstimage2.ifBlank { firstImage2 }
    cat1 = item.cat1.ifBlank { cat1 }
    cat2 = item.cat2.ifBlank { cat2 }
    cat3 = item.cat3.ifBlank { cat3 }
    createdTime = item.createdtime.ifBlank { createdTime }
    modifiedTime = item.modifiedtime.ifBlank { modifiedTime }
    visible = item.showflag != "0"
}
