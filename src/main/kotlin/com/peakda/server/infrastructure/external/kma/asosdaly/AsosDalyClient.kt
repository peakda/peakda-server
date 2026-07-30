package com.peakda.server.infrastructure.external.kma.asosdaly

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.datagokr.DataGoKrBody
import com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.datagokr.getDataGoKrBody
import com.peakda.server.infrastructure.external.kma.asosdaly.response.AsosDalyItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AsosDalyClient(
    @param:Qualifier("asosDalyRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
    private val resilience: ExternalApiResilienceExecutor,
) {
    fun getWthrDataList(params: Map<String, Any?>): DataGoKrBody<AsosDalyItem> = get("/getWthrDataList", params)

    private inline fun <reified T : Any> get(path: String, params: Map<String, Any?>): DataGoKrBody<T> {
        return resilience.execute(PROVIDER) {
            restClient.getDataGoKrBody<T>(objectMapper, errorDecoder, path, params)
        }
    }

    companion object {
        private const val PROVIDER = "KMA"
    }
}
