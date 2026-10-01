package com.peakda.server.infrastructure.external.kma.flower

import com.fasterxml.jackson.databind.ObjectMapper
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerDetail
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerObservationResponse
import com.peakda.server.infrastructure.external.kma.flower.response.FlowerPlace
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 기상청 날씨누리 계절 관측(봄꽃 개화·유명산 단풍) 웹 데이터 클라이언트.
 *
 * 수종 1~3 은 봄꽃 개화 관측(`flower_photojs.jsp`), 수종 [MAPLE_TREE_TYPE] 는 유명산 단풍 관측(`maple_photojs.jsp`)이다.
 * 두 엔드포인트는 응답 구조가 같고 JSONP 함수명만 다르다. 단풍의 `bf/cf/ffShotDate` 는 단풍 전·첫 단풍·단풍 절정이다.
 */
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
        val endpoint = endpointOf(treeType)
        val body = resilience.execute(PROVIDER) {
            restClient.get()
                .uri { builder ->
                    builder.path(endpoint.path)
                        .queryParam("treeType", treeType)
                        .queryParam("obsPlace", obsPlace)
                        .build()
                }
                .retrieve()
                .body(ByteArray::class.java)
        }?.toString(Charsets.UTF_8)
        return parseJsonp(body, endpoint.jsonpPrefix)
    }

    private fun parseJsonp(body: String?, jsonpPrefix: String): FlowerObservationResponse? {
        if (body.isNullOrBlank()) return null

        val start = body.indexOf(jsonpPrefix)
        val end = body.lastIndexOf(')')
        if (start < 0 || end <= start + jsonpPrefix.length) return null

        val json = body.substring(start + jsonpPrefix.length, end).trim()
        if (json.isEmpty()) return null

        return runCatching { objectMapper.readValue(json, FlowerObservationResponse::class.java) }.getOrNull()
    }

    private fun endpointOf(treeType: Int): Endpoint = if (treeType == MAPLE_TREE_TYPE) MAPLE else FLOWER

    private data class Endpoint(val path: String, val jsonpPrefix: String)

    companion object {
        /** 유명산 단풍 관측 수종 번호. 응답의 수종명은 `단풍` 이다. */
        const val MAPLE_TREE_TYPE = 4

        private const val PROVIDER = "KMA"
        private val FLOWER = Endpoint(path = "/flower_photojs.jsp", jsonpPrefix = "applyFlowerData(")
        private val MAPLE = Endpoint(path = "/maple_photojs.jsp", jsonpPrefix = "applyMapleData(")
    }
}
