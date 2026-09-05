package com.peakda.server.infrastructure.external.common

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * provider 단위로 [RateLimiter] 를 등록 / 조회한다.
 *
 * [ExternalRateLimitProperties] 에 등록된 provider 만 토큰 버킷이 적용되고,
 * 그 외는 [RateLimiter.ofDefaults] 가 반환되어 사실상 제한이 없다.
 */
@Component
class ProviderRateLimiterRegistry(
    private val properties: ExternalRateLimitProperties,
) {
    private val registry: RateLimiterRegistry = RateLimiterRegistry.ofDefaults()
    private val cache = ConcurrentHashMap<String, RateLimiter>()

    fun get(provider: String): RateLimiter {
        return cache.computeIfAbsent(provider) { build(provider) }
    }

    private fun build(provider: String): RateLimiter {
        val limit = properties.limitFor(provider)
        if (!properties.enabled || limit == null || limit.permits <= 0) {
            return registry.rateLimiter("default")
        }
        val config = RateLimiterConfig.custom()
            .limitForPeriod(limit.permits)
            .limitRefreshPeriod(limit.period)
            .timeoutDuration(limit.timeout)
            .build()
        return registry.rateLimiter("provider:$provider", config)
    }
}
