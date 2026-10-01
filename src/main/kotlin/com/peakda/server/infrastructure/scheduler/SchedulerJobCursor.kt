package com.peakda.server.infrastructure.scheduler

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

/**
 * 잡이 여러 실행에 걸쳐 이어서 처리할 위치(0-based index)를 보관한다.
 *
 * 한 번에 끝까지 못 도는 순회형 잡(호출 한도에 걸리는 시군구 순회 등)이 매번 처음부터 다시 시작해
 * 목록 뒤쪽이 영영 처리되지 않는 문제를 막는다.
 */
interface SchedulerJobCursor {
    fun load(jobName: String): Int
    fun save(jobName: String, index: Int)
}

@Component
class RedissonSchedulerJobCursor(
    private val redissonClient: RedissonClient,
) : SchedulerJobCursor {
    override fun load(jobName: String): Int =
        redissonClient.getAtomicLong(key(jobName)).get().toInt()

    override fun save(jobName: String, index: Int) {
        redissonClient.getAtomicLong(key(jobName)).set(index.toLong())
    }

    private fun key(jobName: String) = "scheduler:cursor:$jobName"
}
