package com.peakda.server.infrastructure.scheduler

import com.peakda.server.infrastructure.external.common.ExternalApiErrorCode
import com.peakda.server.infrastructure.external.common.ExternalApiException
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRecorder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class JobLoggerTest {

    @Test
    fun `정상 실행 시 start와 complete를 순서대로 기록한다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, AcquiringLock, SchedulerJobSuccessGauge(registry))

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

    /**
     * 미실행 감시(잡이 실패도 안 하고 아예 돌지 않는 경우)의 기준값이라 성공 때마다 갱신되어야 한다.
     */
    @Test
    fun `성공하면 마지막 성공 시각 게이지를 갱신한다`() {
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(RecordingRecorder(), registry, AcquiringLock, SchedulerJobSuccessGauge(registry))
        val before = Instant.now().epochSecond

        logger.run("timedJob") { emptyMap() }

        val recorded = registry.get(SchedulerJobSuccessGauge.METRIC_NAME).tag("job", "timedJob").gauge().value()
        assertThat(recorded).isGreaterThanOrEqualTo(before.toDouble())
    }

    /**
     * 실패한 잡의 게이지가 올라가면 "최근에 성공했다"로 읽혀 미실행 알림이 영구히 침묵한다.
     */
    @Test
    fun `실패한 잡은 마지막 성공 시각 게이지를 만들지 않는다`() {
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(RecordingRecorder(), registry, AcquiringLock, SchedulerJobSuccessGauge(registry))

        logger.run("boomJob") { throw IllegalStateException("boom") }

        assertThat(registry.find(SchedulerJobSuccessGauge.METRIC_NAME).gauge()).isNull()
    }

    @Test
    fun `예외 발생 시 fail을 기록하고 호출자에게 전파하지 않는다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, AcquiringLock, SchedulerJobSuccessGauge(registry))

        logger.run("boomJob") {
            throw IllegalStateException("boom")
        }

        assertThat(recorder.events).containsExactly(
            "start:boomJob",
            "fail:1:IllegalStateException:boom",
        )
        assertThat(registry.counter("scheduler.job.failure_total", "job", "boomJob").count()).isEqualTo(1.0)
    }

    @Test
    fun `비활성 잡은 실행 이력을 남기지 않는다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, AcquiringLock, SchedulerJobSuccessGauge(registry))

        logger.runIfEnabled("disabledJob", false) {
            error("should not run")
        }

        assertThat(recorder.events).isEmpty()
        assertThat(registry.find("scheduler.job.skip_total").counter()).isNull()
    }

    /**
     * `enabled` 는 크론 자동 실행만 제어한다. 관리자가 백오피스에서 누른 수동 실행까지 막으면
     * 202 만 돌아오고 아무 일도 일어나지 않아 운영자가 원인을 알 수 없다.
     */
    @Test
    fun `수동 실행은 비활성 잡도 실행하고 이력을 남긴다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, AcquiringLock, SchedulerJobSuccessGauge(registry))
        var executed = false

        logger.runManually("disabledJob") {
            executed = true
            mapOf(JobLogger.KEY_PROCESSED to 3)
        }

        assertThat(executed).isTrue()
        assertThat(recorder.events).containsExactly(
            "start:disabledJob",
            "complete:1:3:null",
        )
        assertThat(registry.counter("scheduler.job.success_total", "job", "disabledJob").count()).isEqualTo(1.0)
    }

    @Test
    fun `수동 실행도 락 획득 실패 시 skipped locked로 기록한다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, LockedLock, SchedulerJobSuccessGauge(registry))

        logger.runManually("lockedJob") {
            error("should not run")
        }

        assertThat(recorder.events).containsExactly("skip:lockedJob:locked")
        assertThat(registry.counter("scheduler.job.skip_total", "job", "lockedJob", "reason", "locked").count())
            .isEqualTo(1.0)
    }

    @Test
    fun `수동 실행 중 예외는 fail로 기록하고 호출자에게 전파하지 않는다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, AcquiringLock, SchedulerJobSuccessGauge(registry))

        logger.runManually("boomJob") {
            throw IllegalStateException("boom")
        }

        assertThat(recorder.events).containsExactly(
            "start:boomJob",
            "fail:1:IllegalStateException:boom",
        )
        assertThat(registry.counter("scheduler.job.failure_total", "job", "boomJob").count()).isEqualTo(1.0)
    }

    @Test
    fun `QUOTA_EXCEEDED 예외는 실행 row를 skip quota_exhausted로 전환한다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, AcquiringLock, SchedulerJobSuccessGauge(registry))

        logger.run("quotaJob") {
            throw ExternalApiException(ExternalApiErrorCode.EXTERNAL_API_QUOTA_EXCEEDED, "exhausted")
        }

        assertThat(recorder.events).containsExactly(
            "start:quotaJob",
            "skipExisting:1:quota_exhausted",
        )
        assertThat(registry.find("scheduler.job.failure_total").counter()).isNull()
        assertThat(
            registry.counter(
                "scheduler.job.skip_total",
                "job", "quotaJob",
                "reason", "quota_exhausted",
            ).count(),
        ).isEqualTo(1.0)
    }

    @Test
    fun `quota 외 ExternalApiException 은 failure 로 기록한다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, AcquiringLock, SchedulerJobSuccessGauge(registry))

        logger.run("authJob") {
            throw ExternalApiException(ExternalApiErrorCode.EXTERNAL_API_AUTH_FAILED, "auth")
        }

        assertThat(recorder.events.first()).isEqualTo("start:authJob")
        assertThat(recorder.events).anyMatch { it.startsWith("fail:1:ExternalApiException") }
        assertThat(registry.counter("scheduler.job.failure_total", "job", "authJob").count()).isEqualTo(1.0)
    }

    @Test
    fun `락 획득 실패는 실행 이력을 skipped locked로 기록한다`() {
        val recorder = RecordingRecorder()
        val registry = SimpleMeterRegistry()
        val logger = JobLogger(recorder, registry, LockedLock, SchedulerJobSuccessGauge(registry))

        logger.runIfEnabled("lockedJob", true) {
            error("should not run")
        }

        assertThat(recorder.events).containsExactly("skip:lockedJob:locked")
        assertThat(registry.counter("scheduler.job.skip_total", "job", "lockedJob", "reason", "locked").count())
            .isEqualTo(1.0)
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
        override fun skip(jobName: String, reason: String) {
            events += "skip:$jobName:$reason"
        }
        override fun skipExisting(runId: Long?, reason: String) {
            events += "skipExisting:$runId:$reason"
        }
    }

    private object AcquiringLock : SchedulerJobLock {
        override fun <T> withLock(jobName: String, block: () -> T): SchedulerJobLockResult<T> =
            SchedulerJobLockResult.Acquired(block())
    }

    private object LockedLock : SchedulerJobLock {
        override fun <T> withLock(jobName: String, block: () -> T): SchedulerJobLockResult<T> =
            SchedulerJobLockResult.Locked
    }
}
