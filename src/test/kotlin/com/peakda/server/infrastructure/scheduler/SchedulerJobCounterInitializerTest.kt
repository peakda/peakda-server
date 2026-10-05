package com.peakda.server.infrastructure.scheduler

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SchedulerJobCounterInitializerTest {

    @Test
    fun `기동 시 모든 잡의 성공·실패 카운터를 0 으로 만든다`() {
        val meters = SimpleMeterRegistry()
        val jobs = listOf(job("korServiceSync"), job("festivalSync"))

        SchedulerJobCounterInitializer(meters, ManualJobRegistry(jobs)).initialize()

        jobs.forEach { job ->
            assertThat(meters.get(SchedulerJobCounterInitializer.SUCCESS_METRIC).tag("job", job.jobName).counter().count())
                .isZero()
            assertThat(meters.get(SchedulerJobCounterInitializer.FAILURE_METRIC).tag("job", job.jobName).counter().count())
                .isZero()
        }
    }

    @Test
    fun `JobLogger 가 올리는 카운터와 같은 시계열이다`() {
        val meters = SimpleMeterRegistry()
        SchedulerJobCounterInitializer(meters, ManualJobRegistry(listOf(job("boomJob")))).initialize()

        meters.counter("scheduler.job.failure_total", "job", "boomJob").increment()

        assertThat(meters.find(SchedulerJobCounterInitializer.FAILURE_METRIC).counters()).hasSize(1)
        assertThat(meters.get(SchedulerJobCounterInitializer.FAILURE_METRIC).tag("job", "boomJob").counter().count())
            .isEqualTo(1.0)
    }

    private fun job(name: String) = object : ManualTriggerableJob {
        override val jobName = name
        override fun runNow() = Unit
    }
}
