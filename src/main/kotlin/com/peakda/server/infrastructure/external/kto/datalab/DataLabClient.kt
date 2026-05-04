package com.peakda.server.infrastructure.external.kto.datalab

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrBody
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.getDataGoKrBody
import com.peakda.server.infrastructure.external.kto.datalab.response.LocgoVisitrItem
import com.peakda.server.infrastructure.external.kto.datalab.response.MetcoVisitrItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class DataLabClient(
    @param:Qualifier("dataLabRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
) {
    fun metcoRegnVisitrDDList(params: Map<String, Any?>): DataGoKrBody<MetcoVisitrItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/metcoRegnVisitrDDList", params)

    fun locgoRegnVisitrDDList(params: Map<String, Any?>): DataGoKrBody<LocgoVisitrItem> =
        restClient.getDataGoKrBody(objectMapper, errorDecoder, "/locgoRegnVisitrDDList", params)
}
