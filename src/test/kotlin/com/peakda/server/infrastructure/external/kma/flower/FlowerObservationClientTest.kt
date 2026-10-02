package com.peakda.server.infrastructure.external.kma.flower

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class FlowerObservationClientTest {
    private val fixture = clientFixture()
    private val client = fixture.client
    private val server = fixture.server

    @Test
    fun `수종의 장소 목록을 파싱한다`() {
        server.expect(requestTo("https://example.test/flower/flower_photojs.jsp?treeType=1&obsPlace="))
            .andRespond(withSuccess(placesJsonp(), JAVASCRIPT))

        val places = client.getPlaces(1)

        assertThat(places).hasSize(2)
        assertThat(places[0].obsPlace).isEqualTo("여의도 윤중로")
        assertThat(places[0].status).isEqualTo("만발")
    }

    @Test
    fun `장소의 개화 상세를 파싱한다`() {
        server.expect(requestTo("https://example.test/flower/flower_photojs.jsp?treeType=1&obsPlace=%EC%97%AC%EC%9D%98%EB%8F%84%20%EC%9C%A4%EC%A4%91%EB%A1%9C"))
            .andRespond(withSuccess(detailJsonp(), JAVASCRIPT))

        val detail = client.getObservation(1, "여의도 윤중로")

        assertThat(detail?.treeType).isEqualTo("벚나무")
        assertThat(detail?.obsPlace).isEqualTo("여의도 윤중로")
        assertThat(detail?.cfShotDate).isEqualTo("2026-03-29")
        assertThat(detail?.ffShotDate).isEqualTo("2026-04-02")
    }

    @Test
    fun `단풍 수종은 유명산 단풍 엔드포인트에서 받고 첫 단풍·절정일을 파싱한다`() {
        server.expect(requestTo("https://example.test/flower/maple_photojs.jsp?treeType=4&obsPlace="))
            .andRespond(withSuccess(maplePlacesJsonp(), JAVASCRIPT))
        server.expect(requestTo("https://example.test/flower/maple_photojs.jsp?treeType=4&obsPlace=%EC%84%A4%EC%95%85%EC%82%B0"))
            .andRespond(withSuccess(mapleDetailJsonp(), JAVASCRIPT))

        val places = client.getPlaces(FlowerObservationClient.MAPLE_TREE_TYPE)
        val detail = client.getObservation(FlowerObservationClient.MAPLE_TREE_TYPE, "설악산")

        assertThat(places.map { it.obsPlace }).containsExactly("설악산", "오대산")
        assertThat(detail?.treeType).isEqualTo("단풍")
        assertThat(detail?.bfShotDate).isEqualTo("2026-09-11")
        assertThat(detail?.cfShotDate).isEqualTo("2026-09-28")
        assertThat(detail?.ffShotDate).isEmpty()
    }

    @Test
    fun `JSONP 껍데기가 없으면 null을 반환한다`() {
        server.expect(requestTo("https://example.test/flower/flower_photojs.jsp?treeType=1&obsPlace=%EC%97%AC%EC%9D%98%EB%8F%84"))
            .andRespond(withSuccess("{\"flower\": {}}", MediaType.APPLICATION_JSON))

        val detail = client.getObservation(1, "여의도")

        assertThat(detail).isNull()
    }

    @Test
    fun `관측 날짜가 전부 없으면 미관측 장소로 보고 null을 반환한다`() {
        server.expect(requestTo("https://example.test/flower/flower_photojs.jsp?treeType=2&obsPlace=%ED%95%9C%EB%9D%BC%EC%82%B0"))
            .andRespond(
                withSuccess(
                    "applyFlowerData({\"flower\":{\"treeType\":\"철쭉\",\"obsPlace\":\"한라산\",\"bfShotDate\":null,\"cfShotDate\":null,\"ffShotDate\":null}})",
                    JAVASCRIPT,
                ),
            )

        val detail = client.getObservation(2, "한라산")

        assertThat(detail).isNull()
    }

    @Test
    fun `빈 응답이면 빈 장소 목록과 null 상세를 반환한다`() {
        server.expect(requestTo("https://example.test/flower/flower_photojs.jsp?treeType=1&obsPlace="))
            .andRespond(withSuccess("", JAVASCRIPT))
        server.expect(requestTo("https://example.test/flower/flower_photojs.jsp?treeType=1&obsPlace=%EC%97%AC%EC%9D%98%EB%8F%84"))
            .andRespond(withSuccess("", JAVASCRIPT))

        assertThat(client.getPlaces(1)).isEmpty()
        assertThat(client.getObservation(1, "여의도")).isNull()
    }

    private fun clientFixture(): ClientFixture {
        val builder = RestClient.builder().baseUrl("https://example.test/flower")
        val server = MockRestServiceServer.bindTo(builder).build()
        return ClientFixture(
            FlowerObservationClient(
                builder.build(),
                jacksonObjectMapper(),
                ExternalApiResilienceExecutor.noop(),
            ),
            server,
        )
    }

    private fun placesJsonp(): String =
        """
          applyFlowerData({
            "places": [
              { "obsPlace": "여의도 윤중로", "status": "만발", "sts": "3", "serviceFlag": "Y" },
              { "obsPlace": "진해 여좌천", "status": "개화" }
            ],
            "flower": { "obsPlace": "임의 장소", "unused": "ignored" },
            "unused": true
          })
        """.trimIndent()

    private fun maplePlacesJsonp(): String =
        """
          applyMapleData({
            "places": [
              { "obsPlace": "설악산", "status": "첫단풍", "sts": "2" },
              { "obsPlace": "오대산", "status": "단풍전", "sts": "1" }
            ],
            "flower": { "obsPlace": "설악산" }
          })
        """.trimIndent()

    private fun mapleDetailJsonp(): String =
        """
          applyMapleData({
            "places": [],
            "treeType": "4",
            "flower": {
              "treeType": "단풍",
              "obsPlace": "설악산",
              "flowerStatus": "2",
              "bfShotDate": "2026-09-11",
              "cfShotDate": "2026-09-28",
              "ffShotDate": "",
              "modDate": "2026-09-28 10:55:57"
            }
          })
        """.trimIndent()

    private fun detailJsonp(): String =
        """
          applyFlowerData({
            "places": [],
            "flower": {
              "treeType": "벚나무",
              "obsPlace": "여의도 윤중로",
              "obsPlaceDetail": "영등포구 여의서로",
              "flowerStatus": "3",
              "bfShotDate": "2026-03-25",
              "cfShotDate": "2026-03-29",
              "ffShotDate": "2026-04-02",
              "modDate": "2026-04-02 18:10:00",
              "photo": "ignored"
            }
          })
        """.trimIndent()

    private data class ClientFixture(
        val client: FlowerObservationClient,
        val server: MockRestServiceServer,
    )

    companion object {
        private val JAVASCRIPT = MediaType.parseMediaType("application/javascript")
    }
}
