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
    fun `tatsCnctrRateList success decodes items`() {
        server.expect(requestTo("https://example.test/tats/tatsCnctrRateList?serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(successJson(), MediaType.APPLICATION_JSON))

        val body = client.tatsCnctrRateList(emptyMap())

        assertThat(body.item).hasSize(1)
        assertThat(body.item[0].tAtsCd).isEqualTo("T001")
    }

    @Test
    fun `tatsCnctrRateList NODATA returns empty body`() {
        server.expect(requestTo("https://example.test/tats/tatsCnctrRateList?serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(nodataJson(), MediaType.APPLICATION_JSON))

        val body = client.tatsCnctrRateList(emptyMap())

        assertThat(body.item).isEmpty()
    }

    private fun successJson(): String {
        return """
            {
              "response": {
                "header": { "resultCode": "0000", "resultMsg": "OK" },
                "body": {
                  "items": { "item": [ { "baseYmd": "20260503", "tAtsCd": "T001", "tAtsNm": "테스트", "cnctrRate": "45" } ] },
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
}
