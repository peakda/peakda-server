package com.peakda.server.infrastructure.external.datagokr

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.infrastructure.external.common.ExternalApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class DataGoKrRestClientSupportTest {

    private val errorDecoder = DataGoKrErrorDecoder()
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `HTTP 429 응답은 transient ExternalApiException 으로 매핑된다`() {
        val builder = RestClient.builder().baseUrl("https://example.test")
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()

        server.expect(requestTo("https://example.test/probe"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("API rate limit exceeded"))

        assertThatThrownBy {
            client.getDataGoKrBody<TestItem>(objectMapper, errorDecoder, "/probe")
        }.isInstanceOfSatisfying(ExternalApiException::class.java) {
            assertThat(it.errorCode).isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE)
            assertThat(it.message).contains("429")
        }
    }

    @Test
    fun `HTTP 429 응답에 Retry-After 헤더가 있으면 메시지에 포함된다`() {
        val builder = RestClient.builder().baseUrl("https://example.test")
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()

        val headers = HttpHeaders().apply { add("Retry-After", "30") }
        server.expect(requestTo("https://example.test/probe"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body("API rate limit exceeded"))

        assertThatThrownBy {
            client.getDataGoKrBody<TestItem>(objectMapper, errorDecoder, "/probe")
        }.hasMessageContaining("Retry-After=30")
    }

    data class TestItem(val id: String = "")
}
