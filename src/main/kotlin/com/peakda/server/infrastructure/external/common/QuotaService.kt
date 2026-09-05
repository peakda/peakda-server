package com.peakda.server.infrastructure.external.common

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * provider/service/day 단위로 외부 API 호출 횟수를 Redis 카운터로 관리한다.
 *
 * - `tryConsume` 은 호출 직전에 INCR 하고 한도와 비교해 통과/실패를 반환한다.
 * - 카운터 키는 KST 기준 일별로 변경되며 TTL 은 25 시간으로 자동 만료된다.
 * - quota.enabled 가 false 이거나 service 한도가 등록되지 않은 경우 호출은 통과된다.
 */
@Component
open class QuotaService(
    private val redis: StringRedisTemplate,
    private val properties: ExternalQuotaProperties,
    private val clock: Clock = Clock.system(KST),
) {
    open fun tryConsume(provider: String, service: String): Decision {
        val limit = properties.limitFor(provider, service)
        if (!properties.enabled || limit == null || limit <= 0) {
            return Decision.Allowed(used = 0, limit = limit)
        }
        val key = key(provider, service)
        val used = redis.opsForValue().increment(key) ?: return Decision.Allowed(used = 0, limit = limit)
        if (used == 1L) {
            redis.expire(key, EXPIRY)
        }
        return if (used > limit) {
            redis.opsForValue().decrement(key)
            Decision.Exhausted(used = used - 1, limit = limit)
        } else {
            Decision.Allowed(used = used, limit = limit)
        }
    }

    fun currentUsage(provider: String, service: String): Long {
        return redis.opsForValue().get(key(provider, service))?.toLongOrNull() ?: 0L
    }

    private fun key(provider: String, service: String): String {
        val date = LocalDate.now(clock).format(DATE_FORMAT)
        return "quota:$provider:$service:$date"
    }

    sealed interface Decision {
        val used: Long
        val limit: Long?

        data class Allowed(override val used: Long, override val limit: Long?) : Decision
        data class Exhausted(override val used: Long, override val limit: Long?) : Decision
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE
        private val EXPIRY: Duration = Duration.ofHours(25)
    }
}
