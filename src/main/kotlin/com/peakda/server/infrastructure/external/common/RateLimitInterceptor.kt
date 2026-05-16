package com.peakda.server.infrastructure.external.common

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * provider 단위로 토큰을 획득한 뒤 외부 호출을 수행한다.
 * 대기 시간이 초과되면 [ExternalApiException] (UNAVAILABLE) 을 던져 retry 정책이 처리하도록 한다.
 */
class RateLimitInterceptor(
    private val provider: String,
    private val rateLimiterRegistry: ProviderRateLimiterRegistry,
    private val meterRegistry: MeterRegistry,
) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val limiter = rateLimiterRegistry.get(provider)
        try {
            RateLimiter.waitForPermission(limiter)
        } catch (e: RequestNotPermitted) {
            meterRegistry.counter(
                "external.api.rate_limit.rejected_total",
                "provider", provider,
            ).increment()
            log.warn("[external] rate-limit rejected provider={} reason={}", provider, e.message)
            throw ExternalApiException(
                ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE,
                "외부 API 호출이 rate limit 으로 차단되었습니다. provider=$provider",
                e,
            )
        }
        return execution.execute(request, body)
    }

    companion object {
        private val log = LoggerFactory.getLogger(RateLimitInterceptor::class.java)
    }
}
