package com.peakda.server.domain.location.application

import com.peakda.server.domain.location.entity.LocationServiceType
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 같은 사용자가 같은 서비스에서 연속으로 위치정보를 이용할 때 확인자료를 1건으로 합친다.
 *
 * 지도 조회는 pan·zoom 마다 요청이 오므로 요청당 1행을 그대로 쌓으면 확인자료 테이블이 빠르게 커진다.
 * [WINDOW] 안의 재요청은 같은 이용 행위로 보고 첫 건만 남긴다.
 */
@Component
class LocationUsageDebouncer(
    private val redissonClient: RedissonClient,
) {

    /**
     * 이번 요청을 기록해야 하면 true.
     *
     * Redis 가 응답하지 않으면 기록하는 쪽으로 판단한다. 확인자료는 누락이 과기록보다 위험하다.
     */
    fun shouldRecord(userId: Long, service: LocationServiceType): Boolean =
        try {
            redissonClient.getBucket<String>(key(userId, service)).setIfAbsent(MARKER, WINDOW)
        } catch (e: Exception) {
            log.warn("위치정보 이용 기록 디바운스 실패. 기록을 진행한다. userId={}, service={}", userId, service, e)
            true
        }

    private fun key(userId: Long, service: LocationServiceType): String =
        "$KEY_PREFIX$userId:${service.name}"

    companion object {
        private const val KEY_PREFIX = "location-usage:"
        private const val MARKER = "1"
        private val WINDOW: Duration = Duration.ofMinutes(1)
        private val log = LoggerFactory.getLogger(LocationUsageDebouncer::class.java)
    }
}
