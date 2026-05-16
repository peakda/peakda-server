package com.peakda.server.infrastructure.external.kma.midfcst

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrBody
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.common.getDataGoKrBody
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidLandFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidSeaFcstItem
import com.peakda.server.infrastructure.external.kma.midfcst.response.MidTaItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class MidFcstClient(
    @param:Qualifier("midFcstRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
    private val resilience: ExternalApiResilienceExecutor,
) {
    fun getMidFcst(params: Map<String, Any?>): DataGoKrBody<MidFcstItem> = get("/getMidFcst", params)

    fun getMidLandFcst(params: Map<String, Any?>): DataGoKrBody<MidLandFcstItem> = get("/getMidLandFcst", params)

    fun getMidTa(params: Map<String, Any?>): DataGoKrBody<MidTaItem> = get("/getMidTa", params)

    fun getMidSeaFcst(params: Map<String, Any?>): DataGoKrBody<MidSeaFcstItem> = get("/getMidSeaFcst", params)

    private inline fun <reified T : Any> get(path: String, params: Map<String, Any?>): DataGoKrBody<T> {
        return resilience.execute(PROVIDER) {
            restClient.getDataGoKrBody<T>(objectMapper, errorDecoder, path, params)
        }
    }

    companion object {
        private const val PROVIDER = "KMA"
    }
}
