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
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
            assertThat(it.retryAfter).isNull()
        }
    }

    @Test
    fun `HTTP 429 응답의 Retry-After delta-seconds 가 예외에 보존된다`() {
        val builder = RestClient.builder().baseUrl("https://example.test")
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = builder.build()

        val headers = HttpHeaders().apply { add("Retry-After", "30") }
        server.expect(requestTo("https://example.test/probe"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body("API rate limit exceeded"))

        assertThatThrownBy {
            client.getDataGoKrBody<TestItem>(objectMapper, errorDecoder, "/probe")
        }.isInstanceOfSatisfying(ExternalApiException::class.java) {
            assertThat(it.message).contains("Retry-After=30")
            assertThat(it.retryAfter).isEqualTo(Duration.ofSeconds(30))
        }
    }

    @Test
    fun `Retry-After HTTP-date 도 Duration 으로 파싱된다`() {
        val future = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(45)
        val httpDate = future.format(DateTimeFormatter.RFC_1123_DATE_TIME)

        val parsed = parseRetryAfter(httpDate)

        assertThat(parsed).isNotNull()
        assertThat(parsed!!.seconds).isBetween(40L, 50L)
    }

    @Test
    fun `Retry-After 가 음수이거나 유효하지 않으면 null 을 반환한다`() {
        assertThat(parseRetryAfter(null)).isNull()
        assertThat(parseRetryAfter("")).isNull()
        assertThat(parseRetryAfter("   ")).isNull()
        assertThat(parseRetryAfter("-5")).isNull()
        assertThat(parseRetryAfter("not-a-date")).isNull()
    }

    @Test
    fun `Retry-After 가 과거 시각이면 Duration ZERO 가 된다`() {
        val past = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(60)
        val parsed = parseRetryAfter(past.format(DateTimeFormatter.RFC_1123_DATE_TIME))

        assertThat(parsed).isEqualTo(Duration.ZERO)
    }

    data class TestItem(val id: String = "")
}
