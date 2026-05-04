package com.peakda.server.infrastructure.external.kto.photo

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrBody
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.getDataGoKrBody
import com.peakda.server.infrastructure.external.kto.photo.response.GalleryDetailItem
import com.peakda.server.infrastructure.external.kto.photo.response.GalleryListItem
import com.peakda.server.infrastructure.external.kto.photo.response.GallerySearchItem
import com.peakda.server.infrastructure.external.kto.photo.response.GallerySyncDetailItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class PhotoGalleryClient(
    @param:Qualifier("photoGalleryRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
) {
    fun galleryList(params: Map<String, Any?>): DataGoKrBody<GalleryListItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/galleryList1", params)

    fun gallerySearchList(params: Map<String, Any?>): DataGoKrBody<GallerySearchItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/gallerySearchList1", params)

    fun galleryDetailList(params: Map<String, Any?>): DataGoKrBody<GalleryDetailItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/galleryDetailList1", params)

    fun gallerySyncDetailList(params: Map<String, Any?>): DataGoKrBody<GallerySyncDetailItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/gallerySyncDetailList1", params)
}
