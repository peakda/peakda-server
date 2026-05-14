package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.infrastructure.external.kto.korservice.response.AreaBasedSyncListItem

fun AreaBasedSyncListItem.toAttraction(): Attraction = Attraction(
    tourApiContentId = contentid,
    contentTypeCode = contenttypeid.ifBlank { null },
    title = title,
    addressMain = addr1.ifBlank { null },
    addressDetail = addr2.ifBlank { null },
    areaCode = areacode.ifBlank { null },
    sigunguCode = sigungucode.ifBlank { null },
    longitude = mapx.toDoubleOrNull(),
    latitude = mapy.toDoubleOrNull(),
    primaryImageUrl = firstimage.ifBlank { null },
    thumbnailImageUrl = firstimage2.ifBlank { null },
    categoryMajor = cat1.ifBlank { null },
    categoryMedium = cat2.ifBlank { null },
    categoryMinor = cat3.ifBlank { null },
    externalCreatedAt = createdtime.ifBlank { null },
    externalModifiedAt = modifiedtime.ifBlank { null },
    visible = showflag != "0",
)

fun Attraction.applyUpdate(item: AreaBasedSyncListItem) {
    contentTypeCode = item.contenttypeid.ifBlank { contentTypeCode }
    title = item.title
    addressMain = item.addr1.ifBlank { addressMain }
    addressDetail = item.addr2.ifBlank { addressDetail }
    areaCode = item.areacode.ifBlank { areaCode }
    sigunguCode = item.sigungucode.ifBlank { sigunguCode }
    item.mapx.toDoubleOrNull()?.let { longitude = it }
    item.mapy.toDoubleOrNull()?.let { latitude = it }
    primaryImageUrl = item.firstimage.ifBlank { primaryImageUrl }
    thumbnailImageUrl = item.firstimage2.ifBlank { thumbnailImageUrl }
    categoryMajor = item.cat1.ifBlank { categoryMajor }
    categoryMedium = item.cat2.ifBlank { categoryMedium }
    categoryMinor = item.cat3.ifBlank { categoryMinor }
    externalCreatedAt = item.createdtime.ifBlank { externalCreatedAt }
    externalModifiedAt = item.modifiedtime.ifBlank { externalModifiedAt }
    visible = item.showflag != "0"
}
