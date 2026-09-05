package com.peakda.server.infrastructure.external.kto.datalab

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

class DataLabClientTest {
    private val fixture = clientFixture()
    private val client = fixture.client
    private val server = fixture.server

    @Test
    fun `metcoRegnVisitrDDList success decodes items`() {
        server.expect(requestTo("https://example.test/datalab/metcoRegnVisitrDDList?serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(successJson("""{ "baseYmd": "20260503", "areaCd": "11", "areaNm": "서울", "num": "100" }"""), MediaType.APPLICATION_JSON))

        val body = client.metcoRegnVisitrDDList(emptyMap())

        assertThat(body.item).hasSize(1)
        assertThat(body.item[0].areaCd).isEqualTo("11")
    }

    @Test
    fun `locgoRegnVisitrDDList NODATA returns empty body`() {
        server.expect(requestTo("https://example.test/datalab/locgoRegnVisitrDDList?serviceKey=test-key&MobileOS=ETC&MobileApp=peakda-test&_type=json"))
            .andRespond(withSuccess(nodataJson(), MediaType.APPLICATION_JSON))

        val body = client.locgoRegnVisitrDDList(emptyMap())

        assertThat(body.item).isEmpty()
    }

    private fun clientFixture(): ClientFixture {
        val builder = RestClient.builder()
            .baseUrl("https://example.test/datalab")
            .requestInterceptors {
                it.add(ServiceKeyInterceptor("test-key"))
                it.add(KtoCommonParamInterceptor("peakda-test"))
                it.add(JsonOnlyInterceptor())
                it.add(ExternalApiLoggingInterceptor("KTO", "DataLabService"))
            }
        val server = MockRestServiceServer.bindTo(builder).build()
        return ClientFixture(
            DataLabClient(builder.build(), jacksonObjectMapper(), DataGoKrErrorDecoder(), ExternalApiResilienceExecutor.noop()),
            server,
        )
    }

    private fun successJson(item: String): String =
        """{ "response": { "header": { "resultCode": "0000", "resultMsg": "OK" }, "body": { "items": { "item": [ $item ] }, "totalCount": 1 } } }"""

    private fun nodataJson(): String =
        """{ "response": { "header": { "resultCode": "03", "resultMsg": "NODATA_ERROR" }, "body": { "items": { "item": [] } } } }"""

    private data class ClientFixture(
        val client: DataLabClient,
        val server: MockRestServiceServer,
    )
}
