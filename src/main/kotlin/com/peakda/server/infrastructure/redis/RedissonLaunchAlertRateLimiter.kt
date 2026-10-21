package com.peakda.server.infrastructure.redis

import com.peakda.server.domain.launchalert.application.LaunchAlertProperties
import com.peakda.server.domain.launchalert.application.LaunchAlertRateLimiter
import org.redisson.api.RScript
import org.redisson.api.RedissonClient
import org.redisson.client.codec.LongCodec
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 고정 구간 카운터. 증가와 만료 설정을 스크립트 하나로 묶어, 만료 없이 IP 키가 남는 일이 없게 한다.
 *
 * Redis 장애로 신청 자체가 막히지 않도록, 카운트에 실패하면 통과시킨다.
 */
@Component
class RedissonLaunchAlertRateLimiter(
    private val redissonClient: RedissonClient,
    private val properties: LaunchAlertProperties,
) : LaunchAlertRateLimiter {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun tryAcquire(clientIp: String): Boolean = runCatching {
        val count = redissonClient.getScript(LongCodec.INSTANCE).eval<Long>(
            RScript.Mode.READ_WRITE,
            INCREMENT_WITH_TTL,
            RScript.ReturnType.LONG,
            listOf<Any>("launch-alert:rate:$clientIp"),
            properties.rateLimit.window.toMillis(),
        )
        count <= properties.rateLimit.maxPerWindow
    }.getOrElse { e ->
        log.warn("출시 알림 신청 횟수 확인 실패, 제한 없이 통과", e)
        true
    }

    companion object {
        private const val INCREMENT_WITH_TTL = """
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
        """
    }
}
