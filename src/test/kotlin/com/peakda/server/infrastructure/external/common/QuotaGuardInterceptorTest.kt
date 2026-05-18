package com.peakda.server.infrastructure.external.common

import com.peakda.server.common.exception.ErrorCode
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import java.io.ByteArrayInputStream
import java.net.URI

class QuotaGuardInterceptorTest {

    private val request: HttpRequest = TestHttpRequest()
    private val execution = ClientHttpRequestExecution { _, _ -> NoopResponse }

    @Test
    fun `Allowed 일 때 다음 호출로 진행하고 consumed 카운터를 올린다`() {
        val meterRegistry = SimpleMeterRegistry()
        val quotaService = FakeQuotaService(QuotaService.Decision.Allowed(used = 1, limit = 100))
        val interceptor = QuotaGuardInterceptor("KTO", "KorService2", quotaService, meterRegistry)

        val response = interceptor.intercept(request, ByteArray(0), execution)

        assertThat(response).isSameAs(NoopResponse)
        assertThat(
            meterRegistry.counter(
                "external.api.quota.consumed_total",
                "provider", "KTO",
                "service", "KorService2",
            ).count(),
        ).isEqualTo(1.0)
    }

    @Test
    fun `Exhausted 일 때 EXTERNAL_API_QUOTA_EXCEEDED 를 던진다`() {
        val meterRegistry = SimpleMeterRegistry()
        val quotaService = FakeQuotaService(QuotaService.Decision.Exhausted(used = 100, limit = 100))
        val interceptor = QuotaGuardInterceptor("KTO", "KorService2", quotaService, meterRegistry)

        assertThatThrownBy {
            interceptor.intercept(request, ByteArray(0), execution)
        }.isInstanceOf(ExternalApiException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXTERNAL_API_QUOTA_EXCEEDED)

        assertThat(
            meterRegistry.counter(
                "external.api.quota.exhausted_total",
                "provider", "KTO",
                "service", "KorService2",
            ).count(),
        ).isEqualTo(1.0)
    }

    private class FakeQuotaService(private val decision: QuotaService.Decision) :
        QuotaService(Mockito.mock(StringRedisTemplate::class.java), ExternalQuotaProperties()) {
        override fun tryConsume(provider: String, service: String): Decision = decision
    }

    private object NoopResponse : ClientHttpResponse {
        override fun close() {}
        override fun getBody() = ByteArrayInputStream(ByteArray(0))
        override fun getHeaders() = HttpHeaders()
        override fun getStatusCode() = HttpStatus.OK
        override fun getStatusText() = "OK"
    }

    private class TestHttpRequest : HttpRequest {
        override fun getMethod() = HttpMethod.GET
        override fun getURI(): URI = URI.create("https://example.test/")
        override fun getHeaders() = HttpHeaders()
        override fun getAttributes(): MutableMap<String, Any> = mutableMapOf()
    }
}
