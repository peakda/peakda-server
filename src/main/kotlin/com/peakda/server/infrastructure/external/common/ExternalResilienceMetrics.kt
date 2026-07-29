package com.peakda.server.infrastructure.external.common

import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

/**
 * [ProviderResilienceRegistry] 의 서킷브레이커·재시도 상태를 Micrometer 에 노출한다.
 *
 * resilience4j 는 `resilience4j.circuitbreaker.instances.*` 를 Spring 설정으로 쓸 때만
 * 자동설정이 레지스트리를 만들고 메트릭까지 붙여 준다. 이 프로젝트는 provider 별 설정을
 * 자체 프로퍼티로 받아 레지스트리를 직접 만들기 때문에 그 경로를 타지 않는다.
 * 의존성이 있으니 메트릭이 나올 것이라고 기대하면 조용히 비어 있게 된다.
 *
 * 두 binder 는 레지스트리 이벤트를 구독하므로 provider 별 인스턴스가 첫 호출 때
 * 뒤늦게 만들어져도 그 시점에 메트릭이 함께 등록된다.
 */
@Component
class ExternalResilienceMetrics(
    registry: ProviderResilienceRegistry,
    meterRegistry: MeterRegistry,
) {
    init {
        TaggedCircuitBreakerMetrics
            .ofCircuitBreakerRegistry(registry.circuitBreakerRegistry)
            .bindTo(meterRegistry)
        TaggedRetryMetrics
            .ofRetryRegistry(registry.retryRegistry)
            .bindTo(meterRegistry)
    }
}
