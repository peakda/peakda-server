package com.peakda.server.infrastructure.external.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class ExternalApiResilienceExecutorTest {

    private val properties = ExternalResilienceProperties(
        enabled = true,
        providers = mapOf(
            "KTO" to ExternalResilienceProperties.ProviderResilience(
                retry = ExternalResilienceProperties.RetryConfig(
                    maxAttempts = 3,
                    initialInterval = Duration.ofMillis(1),
                    multiplier = 1.0,
                    maxInterval = Duration.ofMillis(1),
                    jitterFactor = 0.0,
                ),
                circuitBreaker = ExternalResilienceProperties.CircuitBreakerConfig(
                    failureRateThreshold = 100f,
                    slowCallRateThreshold = 100f,
                    slowCallDuration = Duration.ofSeconds(30),
                    minimumNumberOfCalls = 1000,
                    slidingWindowSize = 1000,
                    waitDurationInOpenState = Duration.ofSeconds(30),
                    permittedNumberOfCallsInHalfOpenState = 1,
                ),
            ),
        ),
    )

    @Test
    fun `transient UNAVAILABLE 은 maxAttempts 만큼 재시도한다`() {
        val attempts = AtomicInteger()
        val executor = ExternalApiResilienceExecutor(ProviderResilienceRegistry(properties))

        assertThatThrownBy {
            executor.execute<Unit>("KTO") {
                attempts.incrementAndGet()
                throw ExternalApiException(ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE)
            }
        }.isInstanceOf(ExternalApiException::class.java)

        assertThat(attempts.get()).isEqualTo(3)
    }

    @Test
    fun `permanent AUTH_FAILED 은 재시도하지 않는다`() {
        val attempts = AtomicInteger()
        val executor = ExternalApiResilienceExecutor(ProviderResilienceRegistry(properties))

        assertThatThrownBy {
            executor.execute<Unit>("KTO") {
                attempts.incrementAndGet()
                throw ExternalApiException(ExternalApiErrorCode.EXTERNAL_API_AUTH_FAILED)
            }
        }.isInstanceOf(ExternalApiException::class.java)

        assertThat(attempts.get()).isEqualTo(1)
    }

    @Test
    fun `QUOTA_EXCEEDED 은 retry 예산을 낭비하지 않는다`() {
        val attempts = AtomicInteger()
        val executor = ExternalApiResilienceExecutor(ProviderResilienceRegistry(properties))

        assertThatThrownBy {
            executor.execute<Unit>("KTO") {
                attempts.incrementAndGet()
                throw ExternalApiException(ExternalApiErrorCode.EXTERNAL_API_QUOTA_EXCEEDED)
            }
        }.isInstanceOf(ExternalApiException::class.java)

        assertThat(attempts.get()).isEqualTo(1)
    }

    @Test
    fun `properties 비활성 상태에서는 한 번만 실행한다`() {
        val attempts = AtomicInteger()
        val executor = ExternalApiResilienceExecutor.noop()

        assertThatThrownBy {
            executor.execute<Unit>("KTO") {
                attempts.incrementAndGet()
                throw ExternalApiException(ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE)
            }
        }.isInstanceOf(ExternalApiException::class.java)

        assertThat(attempts.get()).isEqualTo(1)
    }
}
