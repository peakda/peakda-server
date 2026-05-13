package com.peakda.server.domain.gallery.application

import com.peakda.server.domain.gallery.entity.GalleryPhoto
import com.peakda.server.infrastructure.external.kto.photo.response.GalleryListItem

fun GalleryListItem.toGalleryPhoto(): GalleryPhoto = GalleryPhoto(
    galContentId = galContentId,
    galContentTypeId = galContentTypeId.ifBlank { null },
    galTitle = galTitle.ifBlank { null },
    galWebImageUrl = galWebImageUrl.ifBlank { null },
    galCreatedTime = galCreatedtime.ifBlank { null },
    galModifiedTime = galModifiedtime.ifBlank { null },
    galPhotographyMonth = galPhotographyMonth.ifBlank { null },
    galPhotographyLocation = galPhotographyLocation.ifBlank { null },
    galPhotographer = galPhotographer.ifBlank { null },
    galSearchKeyword = galSearchKeyword.ifBlank { null },
)

fun GalleryPhoto.applyUpdate(item: GalleryListItem) {
    galContentTypeId = item.galContentTypeId.ifBlank { galContentTypeId }
    galTitle = item.galTitle.ifBlank { galTitle }
    galWebImageUrl = item.galWebImageUrl.ifBlank { galWebImageUrl }
    galCreatedTime = item.galCreatedtime.ifBlank { galCreatedTime }
    galModifiedTime = item.galModifiedtime.ifBlank { galModifiedTime }
    galPhotographyMonth = item.galPhotographyMonth.ifBlank { galPhotographyMonth }
    galPhotographyLocation = item.galPhotographyLocation.ifBlank { galPhotographyLocation }
    galPhotographer = item.galPhotographer.ifBlank { galPhotographer }
    galSearchKeyword = item.galSearchKeyword.ifBlank { galSearchKeyword }
}
