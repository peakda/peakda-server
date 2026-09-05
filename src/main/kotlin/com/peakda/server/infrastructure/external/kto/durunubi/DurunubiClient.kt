package com.peakda.server.infrastructure.external.kto.durunubi

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.datagokr.DataGoKrBody
import com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.datagokr.getDataGoKrBody
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
    private val resilience: ExternalApiResilienceExecutor,
) {
    fun routeList(params: Map<String, Any?>): DataGoKrBody<RouteItem> = get("/routeList", params)

    fun courseList(params: Map<String, Any?>): DataGoKrBody<CourseItem> = get("/courseList", params)

    private inline fun <reified T : Any> get(path: String, params: Map<String, Any?>): DataGoKrBody<T> {
        return resilience.execute(PROVIDER) {
            restClient.getDataGoKrBody<T>(objectMapper, errorDecoder, path, params)
        }
    }

    companion object {
        private const val PROVIDER = "KTO"
    }
}
