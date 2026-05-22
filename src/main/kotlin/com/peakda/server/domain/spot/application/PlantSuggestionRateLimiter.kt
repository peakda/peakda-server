package com.peakda.server.domain.spot.application

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * 사용자별 식물 제안 rate limit. 외부 API 용 [com.peakda.server.infrastructure.external.common.ProviderRateLimiterRegistry]
 * 와 책임을 분리하기 위해 spot 도메인 내부에 둔다. 인메모리이며, 다중 인스턴스/재시작 시 카운트가 리셋된다.
 */
@Component
class PlantSuggestionRateLimiter(
    private val properties: PlantSuggestionRateLimitProperties,
) {
    private val registry: RateLimiterRegistry = RateLimiterRegistry.ofDefaults()
    private val limiters = ConcurrentHashMap<Long, RateLimiter>()

    fun tryAcquire(userId: Long): Boolean {
        val limiter = limiters.computeIfAbsent(userId) { build(userId) }
        return limiter.acquirePermission()
    }

    private fun build(userId: Long): RateLimiter {
        val config = RateLimiterConfig.custom()
            .limitForPeriod(properties.maxPerWindow)
            .limitRefreshPeriod(properties.window)
            .timeoutDuration(Duration.ZERO)
            .build()
        return registry.rateLimiter("plant-suggestion:$userId", config)
    }
}
