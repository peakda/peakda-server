package com.peakda.server.domain.gallery.application

import com.peakda.server.domain.gallery.entity.GalleryPhoto
import com.peakda.server.infrastructure.external.kto.photo.response.GalleryListItem

fun GalleryListItem.toGalleryPhoto(): GalleryPhoto = GalleryPhoto(
    tourApiContentId = galContentId,
    contentTypeCode = galContentTypeId.ifBlank { null },
    title = galTitle.ifBlank { null },
    webImageUrl = galWebImageUrl.ifBlank { null },
    externalCreatedAt = galCreatedtime.ifBlank { null },
    externalModifiedAt = galModifiedtime.ifBlank { null },
    photographyMonth = galPhotographyMonth.ifBlank { null },
    photographyLocation = galPhotographyLocation.ifBlank { null },
    photographer = galPhotographer.ifBlank { null },
    searchKeyword = galSearchKeyword.ifBlank { null },
)

fun GalleryPhoto.applyUpdate(item: GalleryListItem) {
    contentTypeCode = item.galContentTypeId.ifBlank { contentTypeCode }
    title = item.galTitle.ifBlank { title }
    webImageUrl = item.galWebImageUrl.ifBlank { webImageUrl }
    externalCreatedAt = item.galCreatedtime.ifBlank { externalCreatedAt }
    externalModifiedAt = item.galModifiedtime.ifBlank { externalModifiedAt }
    photographyMonth = item.galPhotographyMonth.ifBlank { photographyMonth }
    photographyLocation = item.galPhotographyLocation.ifBlank { photographyLocation }
    photographer = item.galPhotographer.ifBlank { photographer }
    searchKeyword = item.galSearchKeyword.ifBlank { searchKeyword }
}
