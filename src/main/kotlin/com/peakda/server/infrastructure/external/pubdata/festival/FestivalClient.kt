package com.peakda.server.infrastructure.external.pubdata.festival

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrBody
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.getDataGoKrBody
import com.peakda.server.infrastructure.external.pubdata.festival.response.FestivalItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class FestivalClient(
    @param:Qualifier("festivalRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
) {
    fun list(params: Map<String, Any?>): DataGoKrBody<FestivalItem> {
        return restClient.getDataGoKrBody(objectMapper, errorDecoder, "", params)
    }
}
