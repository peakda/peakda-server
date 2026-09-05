package com.peakda.server.infrastructure.external.kto.korservice

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.peakda.server.infrastructure.external.datagokr.DataGoKrErrorDecoder
import com.peakda.server.infrastructure.external.common.ExternalApiLoggingInterceptor
import com.peakda.server.infrastructure.external.common.ExternalApiResilienceExecutor
import com.peakda.server.infrastructure.external.common.JsonOnlyInterceptor
import com.peakda.server.infrastructure.external.common.KtoCommonParamInterceptor
import com.peakda.server.infrastructure.external.common.ServiceKeyInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class KorServiceClientTest {
    private val fixture = clientFixture()
    private val client = fixture.client
    private val server = fixture.server

    @Test
    fun `areaBasedList2 success decodes items`() {
        server.expect(
            requestTo(
                "https://example.test/kto/areaBasedList2?numOfRows=10&pageNo=1&serviceKey=test%2Bkey&MobileOS=ETC&MobileApp=peakda-test&_type=json"
            )
        ).andRespond(withSuccess(successJson(), MediaType.APPLICATION_JSON))

        val body = client.areaBasedList(mapOf("numOfRows" to 10, "pageNo" to 1))

        assertThat(body.totalCount).isEqualTo(1)
        assertThat(body.item).hasSize(1)
        assertThat(body.item[0].contentid).isEqualTo("126128")
        assertThat(body.item[0].title).isEqualTo("테스트 명소")
    }

    @Test
    fun `areaBasedList2 NODATA returns empty body`() {
        server.expect(requestTo("https://example.test/kto/areaBasedList2?serviceKey=test%2Bkey&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(envelopeJson("03", "NODATA_ERROR"), MediaType.APPLICATION_JSON))

        val body = client.areaBasedList(emptyMap())

        assertThat(body.item).isEmpty()
        assertThat(body.totalCount).isZero()
    }

    @Test
    fun `areaBasedList2 XML error is converted`() {
        server.expect(requestTo("https://example.test/kto/areaBasedList2?serviceKey=test%2Bkey&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(xmlError(), MediaType.APPLICATION_XML))

        assertThatThrownBy { client.areaBasedList(emptyMap()) }
            .hasMessageContaining("LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS")
    }

    private fun successJson(): String {
        return """
            {
              "response": {
                "header": { "resultCode": "0000", "resultMsg": "OK" },
                "body": {
                  "items": {
                    "item": [
                      {
                        "contentid": "126128",
                        "contenttypeid": "12",
                        "title": "테스트 명소",
                        "mapx": "126.981611",
                        "mapy": "37.568477"
                      }
                    ]
                  },
                  "numOfRows": 10,
                  "pageNo": 1,
                  "totalCount": 1
                }
              }
            }
        """.trimIndent()
    }

    private fun envelopeJson(resultCode: String, resultMsg: String): String {
        return """
            {
              "response": {
                "header": { "resultCode": "$resultCode", "resultMsg": "$resultMsg" },
                "body": { "items": { "item": [] }, "numOfRows": 0, "pageNo": 0, "totalCount": 0 }
              }
            }
        """.trimIndent()
    }

    private fun xmlError(): String {
        return """
            <OpenAPI_ServiceResponse>
              <cmmMsgHeader>
                <errMsg>SERVICE ERROR</errMsg>
                <returnAuthMsg>LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS</returnAuthMsg>
                <returnReasonCode>22</returnReasonCode>
              </cmmMsgHeader>
            </OpenAPI_ServiceResponse>
        """.trimIndent()
    }

    private fun clientFixture(): ClientFixture {
        val builder = RestClient.builder()
            .baseUrl("https://example.test/kto")
            .requestInterceptors {
                it.add(ServiceKeyInterceptor("test%2Bkey"))
                it.add(KtoCommonParamInterceptor("peakda-test"))
                it.add(JsonOnlyInterceptor())
                it.add(ExternalApiLoggingInterceptor("KTO", "KorService2"))
            }
        val server = MockRestServiceServer.bindTo(builder).build()
        return ClientFixture(
            KorServiceClient(builder.build(), jacksonObjectMapper(), DataGoKrErrorDecoder(), ExternalApiResilienceExecutor.noop()),
            server,
        )
    }

    private data class ClientFixture(
        val client: KorServiceClient,
        val server: MockRestServiceServer,
    )
}
