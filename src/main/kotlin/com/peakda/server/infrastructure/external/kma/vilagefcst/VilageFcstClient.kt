package com.peakda.server.infrastructure.external.kma.vilagefcst

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.DataGoKrBody
import com.peakda.server.infrastructure.external.common.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.getDataGoKrBody
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.FcstVersionItem
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.UltraSrtFcstItem
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.UltraSrtNcstItem
import com.peakda.server.infrastructure.external.kma.vilagefcst.response.VilageFcstItem
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class VilageFcstClient(
    @param:Qualifier("vilageFcstRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val errorDecoder: DataGoKrErrorDecoder,
) {
    fun getVilageFcst(params: Map<String, Any?>): DataGoKrBody<VilageFcstItem> = get("/getVilageFcst", params)

    fun getUltraSrtFcst(params: Map<String, Any?>): DataGoKrBody<UltraSrtFcstItem> = get("/getUltraSrtFcst", params)

    fun getUltraSrtNcst(params: Map<String, Any?>): DataGoKrBody<UltraSrtNcstItem> = get("/getUltraSrtNcst", params)

    fun getFcstVersion(params: Map<String, Any?>): DataGoKrBody<FcstVersionItem> = get("/getFcstVersion", params)

    private inline fun <reified T : Any> get(path: String, params: Map<String, Any?>): DataGoKrBody<T> {
        return restClient.getDataGoKrBody(objectMapper, errorDecoder, path, params)
    }
}
