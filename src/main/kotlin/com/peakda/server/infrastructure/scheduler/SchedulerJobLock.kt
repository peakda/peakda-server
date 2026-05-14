package com.peakda.server.infrastructure.scheduler

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

interface SchedulerJobLock {
    fun <T> withLock(jobName: String, block: () -> T): SchedulerJobLockResult<T>
}

sealed interface SchedulerJobLockResult<out T> {
    data class Acquired<T>(val value: T) : SchedulerJobLockResult<T>
    data object Locked : SchedulerJobLockResult<Nothing>
}

@Component
class RedissonSchedulerJobLock(
    private val redissonClient: RedissonClient,
) : SchedulerJobLock {
    override fun <T> withLock(jobName: String, block: () -> T): SchedulerJobLockResult<T> {
        val lock = redissonClient.getLock("scheduler:job:$jobName")
        val acquired = try {
            lock.tryLock(0, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!acquired) return SchedulerJobLockResult.Locked

        try {
            return SchedulerJobLockResult.Acquired(block())
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }
}
