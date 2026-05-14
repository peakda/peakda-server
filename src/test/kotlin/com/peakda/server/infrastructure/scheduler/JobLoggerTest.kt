package com.peakda.server.infrastructure.scheduler

import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRecorder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JobLoggerTest {

    @Test
    fun `정상 실행 시 start와 complete를 순서대로 기록한다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry)

        logger.run("testJob") {
            mapOf(JobLogger.KEY_PROCESSED to 7, JobLogger.KEY_TOTAL to 42)
        }

        assertThat(recorder.events).containsExactly(
            "start:testJob",
            "complete:1:7:42",
        )
        assertThat(registry.counter("scheduler.job.success_total", "job", "testJob").count()).isEqualTo(1.0)
        assertThat(registry.timer("scheduler.job.duration", "job", "testJob").count()).isEqualTo(1L)
    }

    @Test
    fun `예외 발생 시 fail을 기록하고 호출자에게 전파하지 않는다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry)

        logger.run("boomJob") {
            throw IllegalStateException("boom")
        }

        assertThat(recorder.events).containsExactly(
            "start:boomJob",
            "fail:1:IllegalStateException:boom",
        )
        assertThat(registry.counter("scheduler.job.failure_total", "job", "boomJob").count()).isEqualTo(1.0)
    }

    private class RecordingRecorder : SchedulerJobRunRecorder {
        val events = mutableListOf<String>()
        private var nextId = 1L
        override fun start(jobName: String): Long {
            val id = nextId++
            events += "start:$jobName"
            return id
        }
        override fun complete(runId: Long?, processedCount: Int?, totalCount: Int?) {
            events += "complete:$runId:$processedCount:$totalCount"
        }
        override fun fail(runId: Long?, throwable: Throwable) {
            events += "fail:$runId:${throwable::class.simpleName}:${throwable.message}"
        }
    }
}
