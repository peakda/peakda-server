package com.peakda.server.infrastructure.external.common

import com.peakda.server.common.exception.ErrorCode
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import java.util.concurrent.ConcurrentHashMap

/**
 * provider 단위 [Retry] / [CircuitBreaker] 를 등록하고 캐시한다.
 *
 * - Retry: transient 오류만 재시도. AUTH/QUOTA/BAD_REQUEST 등 permanent 오류는 재시도하지 않는다.
 * - CircuitBreaker: 동일한 transient 오류 집합과 transport 예외만 실패로 기록한다.
 *   permanent 오류는 외부 시스템 장애가 아니므로 회로 차단 통계에서 제외된다.
 */
@Component
class ProviderResilienceRegistry(
    private val properties: ExternalResilienceProperties,
) {
    private val retryRegistry: RetryRegistry = RetryRegistry.ofDefaults()
    private val circuitBreakerRegistry: CircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
    private val retryCache = ConcurrentHashMap<String, Retry>()
    private val cbCache = ConcurrentHashMap<String, CircuitBreaker>()

    fun retry(provider: String): Retry = retryCache.computeIfAbsent(provider) { buildRetry(provider) }

    fun circuitBreaker(provider: String): CircuitBreaker =
        cbCache.computeIfAbsent(provider) { buildCircuitBreaker(provider) }

    private fun buildRetry(provider: String): Retry {
        val resilience = properties.forProvider(provider)
        if (!properties.enabled || resilience == null) {
            return retryRegistry.retry("default-noop", RetryConfig.custom<Any>().maxAttempts(1).build())
        }
        val retry = resilience.retry
        val config = RetryConfig.custom<Any>()
            .maxAttempts(retry.maxAttempts)
            .intervalFunction(
                IntervalFunction.ofExponentialRandomBackoff(
                    retry.initialInterval,
                    retry.multiplier,
                    retry.jitterFactor,
                    retry.maxInterval,
                ),
            )
            .retryOnException(::isTransient)
            .build()
        return retryRegistry.retry("provider:$provider", config)
    }

    private fun buildCircuitBreaker(provider: String): CircuitBreaker {
        val resilience = properties.forProvider(provider)
        if (!properties.enabled || resilience == null) {
            return circuitBreakerRegistry.circuitBreaker("default-disabled")
        }
        val cb = resilience.circuitBreaker
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(cb.failureRateThreshold)
            .slowCallRateThreshold(cb.slowCallRateThreshold)
            .slowCallDurationThreshold(cb.slowCallDuration)
            .minimumNumberOfCalls(cb.minimumNumberOfCalls)
            .slidingWindowSize(cb.slidingWindowSize)
            .waitDurationInOpenState(cb.waitDurationInOpenState)
            .permittedNumberOfCallsInHalfOpenState(cb.permittedNumberOfCallsInHalfOpenState)
            .recordException(::isTransient)
            .build()
        return circuitBreakerRegistry.circuitBreaker("provider:$provider", config)
    }

    /**
     * 일시적 외부 장애 여부. retry/CB 둘 다 같은 분류를 사용한다.
     * - transient: UNAVAILABLE, TIMEOUT, INVALID_RESPONSE 와 transport 예외
     * - permanent: AUTH_FAILED, QUOTA_EXCEEDED, BAD_REQUEST
     */
    private fun isTransient(throwable: Throwable): Boolean {
        return when (throwable) {
            is ExternalApiException -> when (throwable.errorCode) {
                ErrorCode.EXTERNAL_API_UNAVAILABLE -> true
                ErrorCode.EXTERNAL_API_TIMEOUT -> true
                ErrorCode.EXTERNAL_API_INVALID_RESPONSE -> true
                else -> false
            }
            is ResourceAccessException -> true
            else -> false
        }
    }
}
