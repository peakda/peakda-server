package com.peakda.server.infrastructure.external.common

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * 요청 직전 [QuotaService] 로 카운터를 1 증가시키고 한도를 검사한다.
 * 초과 시 [ExternalApiException] (QUOTA_EXCEEDED) 을 던져 외부 호출 자체를 막는다.
 *
 * data.go.kr 은 실패 응답도 호출 수로 집계할 수 있어 over-count 가 안전하다.
 * 다만 한도 초과로 막힌 호출은 카운터를 되돌려 인접 호출이 부당하게 차단되지 않게 한다.
 */
class QuotaGuardInterceptor(
    private val provider: String,
    private val service: String,
    private val quotaService: QuotaService,
    private val meterRegistry: MeterRegistry,
) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        when (val decision = quotaService.tryConsume(provider, service)) {
            is QuotaService.Decision.Allowed -> {
                meterRegistry.counter(
                    "external.api.quota.consumed_total",
                    "provider", provider,
                    "service", service,
                ).increment()
            }
            is QuotaService.Decision.Exhausted -> {
                meterRegistry.counter(
                    "external.api.quota.exhausted_total",
                    "provider", provider,
                    "service", service,
                ).increment()
                log.warn(
                    "[external] quota exhausted provider={} service={} used={} limit={}",
                    provider, service, decision.used, decision.limit,
                )
                throw ExternalApiException(
                    ExternalApiErrorCode.EXTERNAL_API_QUOTA_EXCEEDED,
                    "외부 API 일일 호출 한도를 초과했습니다. provider=$provider service=$service",
                )
            }
        }
        return execution.execute(request, body)
    }

    companion object {
        private val log = LoggerFactory.getLogger(QuotaGuardInterceptor::class.java)
    }
}
