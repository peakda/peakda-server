package com.peakda.server.infrastructure.external.kto.durunubi

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrBody
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.getDataGoKrBody
import com.peakda.server.infrastructure.external.kto.durunubi.response.CourseItem
import com.peakda.server.infrastructure.external.kto.durunubi.response.RouteItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class DurunubiClient(
    @param:Qualifier("durunubiRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
) {
    fun routeList(params: Map<String, Any?>): DataGoKrBody<RouteItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/routeList", params)

    fun courseList(params: Map<String, Any?>): DataGoKrBody<CourseItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/courseList", params)
}
