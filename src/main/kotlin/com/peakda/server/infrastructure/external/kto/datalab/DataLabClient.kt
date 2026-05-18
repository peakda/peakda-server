package com.peakda.server.infrastructure.external.kto.datalab

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.datagokr.DataGoKrBody
import com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.datagokr.getDataGoKrBody
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
    private val resilience: ExternalApiResilienceExecutor,
) {
    fun metcoRegnVisitrDDList(params: Map<String, Any?>): DataGoKrBody<MetcoVisitrItem> =
        get("/metcoRegnVisitrDDList", params)

    fun locgoRegnVisitrDDList(params: Map<String, Any?>): DataGoKrBody<LocgoVisitrItem> =
        get("/locgoRegnVisitrDDList", params)

    private inline fun <reified T : Any> get(path: String, params: Map<String, Any?>): DataGoKrBody<T> {
        return resilience.execute(PROVIDER) {
            restClient.getDataGoKrBody<T>(objectMapper, errorDecoder, path, params)
        }
    }

    companion object {
        private const val PROVIDER = "KTO"
    }
}
