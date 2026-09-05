package com.peakda.server.infrastructure.external.common

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.function.Supplier

/**
 * provider 단위로 [CircuitBreaker] 가 [Retry] 를 감싸는 형태로 외부 호출을 실행한다.
 *
 * - CB 가 OPEN 이면 즉시 [ExternalApiException] (UNAVAILABLE) 으로 변환된다.
 * - retry 는 transient 오류만 재시도하고, AUTH/QUOTA/BAD_REQUEST 는 즉시 전파한다.
 * - CB outside Retry: 한 호출의 최종 outcome 만 회로 통계에 반영되어 노이즈가 적다.
 */
@Component
class ExternalApiResilienceExecutor(
    private val registry: ProviderResilienceRegistry,
) {
    fun <T> execute(provider: String, block: () -> T): T {
        val retry = registry.retry(provider)
        val circuitBreaker = registry.circuitBreaker(provider)
        val supplier: Supplier<T> = Retry.decorateSupplier(retry) { block() }
        val decorated: Supplier<T> = CircuitBreaker.decorateSupplier(circuitBreaker, supplier)
        return try {
            decorated.get()
        } catch (e: CallNotPermittedException) {
            log.warn("[external] circuit-breaker open provider={} reason={}", provider, e.message)
            throw ExternalApiException(
                ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE,
                "외부 API circuit breaker 가 열려 호출을 차단했습니다. provider=$provider",
                e,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExternalApiResilienceExecutor::class.java)

        /** 테스트용 no-op executor. retry/CB 비활성화 정책으로 등록되어 block 을 1회 실행한다. */
        fun noop(): ExternalApiResilienceExecutor =
            ExternalApiResilienceExecutor(ProviderResilienceRegistry(ExternalResilienceProperties()))
    }
}
