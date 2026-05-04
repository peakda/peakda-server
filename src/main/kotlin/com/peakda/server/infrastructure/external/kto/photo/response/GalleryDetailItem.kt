package com.peakda.server.infrastructure.external.kto.photo.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GalleryDetailItem(
    val galContentId: String = "",
    val galContentTypeId: String = "",
    val galTitle: String = "",
    val galWebImageUrl: String = "",
    val galCreatedtime: String = "",
    val galModifiedtime: String = "",
    val galPhotographyMonth: String = "",
    val galPhotographyLocation: String = "",
    val galPhotographer: String = "",
    val galSearchKeyword: String = "",
)
