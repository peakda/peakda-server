package com.peakda.server.infrastructure.external.kma.flower

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerDetail
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerObservationResponse
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerPlace
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class FlowerObservationClient(
    @param:Qualifier("flowerObservationRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val resilience: ExternalApiResilienceExecutor,
) {
    /** 수종의 장소 목록. */
    fun getPlaces(treeType: Int): List<FlowerPlace> {
        return getResponse(treeType, "")?.places.orEmpty()
    }

    /** 장소 상세. 파싱 불가·미관측이면 null. */
    fun getObservation(treeType: Int, obsPlace: String): FlowerDetail? {
        return getResponse(treeType, obsPlace)?.flower?.takeIf { detail ->
            listOf(detail.bfShotDate, detail.cfShotDate, detail.ffShotDate).any { !it.isNullOrBlank() }
        }
    }

    private fun getResponse(treeType: Int, obsPlace: String): FlowerObservationResponse? {
        val body = resilience.execute(PROVIDER) {
            restClient.get()
                .uri { builder ->
                    builder.path(PATH)
                        .queryParam("treeType", treeType)
                        .queryParam("obsPlace", obsPlace)
                        .build()
                }
                .retrieve()
                .body(ByteArray::class.java)
        }?.toString(Charsets.UTF_8)
        return parseJsonp(body)
    }

    private fun parseJsonp(body: String?): FlowerObservationResponse? {
        if (body.isNullOrBlank()) return null

        val start = body.indexOf(JSONP_PREFIX)
        val end = body.lastIndexOf(')')
        if (start < 0 || end <= start + JSONP_PREFIX.length) return null

        val json = body.substring(start + JSONP_PREFIX.length, end).trim()
        if (json.isEmpty()) return null

        return runCatching { objectMapper.readValue(json, FlowerObservationResponse::class.java) }.getOrNull()
    }

    companion object {
        private const val PROVIDER = "KMA"
        private const val PATH = "/flower_photojs.jsp"
        private const val JSONP_PREFIX = "applyFlowerData("
    }
}
