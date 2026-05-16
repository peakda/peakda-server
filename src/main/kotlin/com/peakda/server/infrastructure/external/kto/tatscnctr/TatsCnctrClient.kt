package com.peakda.server.infrastructure.external.kto.tatscnctr

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrBody
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.common.getDataGoKrBody
import com.peakda.server.infrastructure.external.kto.tatscnctr.response.CnctrRateItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TatsCnctrClient(
    @param:Qualifier("tatsCnctrRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
    private val resilience: ExternalApiResilienceExecutor,
) {
    fun tatsCnctrRateList(params: Map<String, Any?>): DataGoKrBody<CnctrRateItem> {
        return resilience.execute(PROVIDER) {
            restClient.getDataGoKrBody<CnctrRateItem>(objectMapper, errorDecoder, "/tatsCnctrRateList", params)
        }
    }

    companion object {
        private const val PROVIDER = "KTO"
    }
}
