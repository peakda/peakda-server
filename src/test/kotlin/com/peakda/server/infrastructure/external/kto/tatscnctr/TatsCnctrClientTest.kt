package com.peakda.server.infrastructure.external.kto.tatscnctr

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiLoggingInterceptor
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.common.JsonOnlyInterceptor
import com.peakda.server.infrastructure.external.common.KtoCommonParamInterceptor
import com.peakda.server.infrastructure.external.common.ServiceKeyInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class TatsCnctrClientTest {
    private val fixture = clientFixture()
    private val client = fixture.client
    private val server = fixture.server

    @Test
    fun `tatsCnctrRatedList 는 필수 파라미터 areaCd signguCd 를 붙여 호출한다`() {
        server.expect(requestTo(EXPECTED_URI))
            .andRespond(withSuccess(successJson(), MediaType.APPLICATION_JSON))

        val body = client.tatsCnctrRatedList(REGION_PARAMS)

        server.verify()
        assertThat(body.item).hasSize(1)
        assertThat(body.item[0].tAtsNm).isEqualTo("간현관광지")
        assertThat(body.item[0].baseYmd).isEqualTo("20260515")
        assertThat(body.item[0].cnctrRate).isEqualTo("46.71")
    }

    @Test
    fun `tatsCnctrRatedList NODATA returns empty body`() {
        server.expect(requestTo(EXPECTED_URI))
            .andRespond(withSuccess(nodataJson(), MediaType.APPLICATION_JSON))

        val body = client.tatsCnctrRatedList(REGION_PARAMS)

        assertThat(body.item).isEmpty()
    }

    /** 응답에 관광지 코드 필드가 없다. 자연키는 지역·시군구·관광지명으로만 만들 수 있다. */
    private fun successJson(): String {
        return """
            {
              "response": {
                "header": { "resultCode": "0000", "resultMsg": "OK" },
                "body": {
                  "items": { "item": [ {
                    "baseYmd": "20260515",
                    "areaCd": "51",
                    "areaNm": "강원특별자치도",
                    "signguCd": "51130",
                    "signguNm": "원주시",
                    "tAtsNm": "간현관광지",
                    "cnctrRate": "46.71"
                  } ] },
                  "numOfRows": 10,
                  "pageNo": 1,
                  "totalCount": 1
                }
              }
            }
        """.trimIndent()
    }

    private fun nodataJson(): String {
        return """
            { "response": { "header": { "resultCode": "03", "resultMsg": "NODATA_ERROR" }, "body": { "items": { "item": [] } } } }
        """.trimIndent()
    }

    private fun clientFixture(): ClientFixture {
        val builder = RestClient.builder()
            .baseUrl("https://example.test/tats")
            .requestInterceptors {
                it.add(ServiceKeyInterceptor("test-key"))
                it.add(KtoCommonParamInterceptor("peakda-test"))
                it.add(JsonOnlyInterceptor())
                it.add(ExternalApiLoggingInterceptor("KTO", "TatsCnctrRateService"))
            }
        val server = MockRestServiceServer.bindTo(builder).build()
        return ClientFixture(
            TatsCnctrClient(builder.build(), jacksonObjectMapper(), DataGoKrErrorDecoder(), ExternalApiResilienceExecutor.noop()),
            server,
        )
    }

    private data class ClientFixture(
        val client: TatsCnctrClient,
        val server: MockRestServiceServer,
    )

    companion object {
        private val REGION_PARAMS = mapOf("areaCd" to "51", "signguCd" to "51130")
        private const val EXPECTED_URI =
            "https://example.test/tats/tatsCnctrRatedList" +
                "?areaCd=51&signguCd=51130&serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"
    }
}
