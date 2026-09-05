package com.peakda.server.infrastructure.external.common

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * provider 단위 retry / circuit breaker 정책. transient 오류(`UNAVAILABLE`, `TIMEOUT`,
 * `INVALID_RESPONSE`)에는 지수 백오프 + jitter 로 재시도하고, permanent 오류
 * (`AUTH_FAILED`, `QUOTA_EXCEEDED`, `BAD_REQUEST`)는 즉시 실패한다. circuit breaker 는
 * provider 단위로 실패율을 측정해 차단한다.
 */
@ConfigurationProperties(prefix = "external.resilience")
data class ExternalResilienceProperties(
    val enabled: Boolean = false,
    val providers: Map<String, ProviderResilience> = emptyMap(),
) {
    fun forProvider(provider: String): ProviderResilience? = providers[provider]

    data class ProviderResilience(
        val retry: RetryConfig = RetryConfig(),
        val circuitBreaker: CircuitBreakerConfig = CircuitBreakerConfig(),
    )

    data class RetryConfig(
        val maxAttempts: Int = 3,
        val initialInterval: Duration = Duration.ofMillis(500),
        val multiplier: Double = 2.0,
        val maxInterval: Duration = Duration.ofSeconds(5),
        val jitterFactor: Double = 0.3,
    )

    data class CircuitBreakerConfig(
        val failureRateThreshold: Float = 50f,
        val slowCallRateThreshold: Float = 100f,
        val slowCallDuration: Duration = Duration.ofSeconds(10),
        val minimumNumberOfCalls: Int = 10,
        val slidingWindowSize: Int = 20,
        val waitDurationInOpenState: Duration = Duration.ofSeconds(30),
        val permittedNumberOfCallsInHalfOpenState: Int = 3,
    )
}
