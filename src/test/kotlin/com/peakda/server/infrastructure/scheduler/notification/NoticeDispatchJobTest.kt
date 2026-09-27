package com.peakda.server.infrastructure.scheduler.notification

import com.peakda.server.domain.notification.application.NoticeFanoutService
import com.peakda.server.infrastructure.scheduler.JobLogger
import com.peakda.server.infrastructure.scheduler.NoOpSchedulerJobLock
import com.peakda.server.infrastructure.scheduler.SchedulerJobSuccessGauge
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRecorder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails

class NoticeDispatchJobTest {

    private var pending = false
    private val service = mock(NoticeFanoutService::class.java) { invocation ->
        when (invocation.method.name) {
            "hasPendingDispatch" -> pending
            "dispatchPending" -> 3
            else -> null
        }
    }
    private val recorder = RecordingRecorder()
    private val registry = SimpleMeterRegistry()
    private val jobLogger = JobLogger(recorder, registry, NoOpSchedulerJobLock, SchedulerJobSuccessGauge(registry))

    @Test
    fun `발송 대기 공지가 없으면 발송도 실행 이력도 남기지 않는다`() {
        NoticeDispatchJob(service, props(jobEnabled = true), jobLogger).run()

        assertThat(recorder.started).isEmpty()
        assertThat(mockingDetails(service).invocations.map { it.method.name }).containsExactly("hasPendingDispatch")
    }

    @Test
    fun `발송 대기 공지가 있으면 발송하고 실행 이력을 남긴다`() {
        pending = true

        NoticeDispatchJob(service, props(jobEnabled = true), jobLogger).run()

        assertThat(recorder.started).containsExactly("noticeDispatch")
        assertThat(mockingDetails(service).invocations.map { it.method.name })
            .containsExactly("hasPendingDispatch", "dispatchPending")
    }

    @Test
    fun `비활성화되면 대기 공지를 조회하지 않는다`() {
        pending = true

        NoticeDispatchJob(service, props(jobEnabled = false), jobLogger).run()

        assertThat(recorder.started).isEmpty()
        assertThat(mockingDetails(service).invocations).isEmpty()
    }

    @Test
    fun `수동 실행은 대기 공지가 없어도 실행 이력을 남긴다`() {
        NoticeDispatchJob(service, props(jobEnabled = true), jobLogger).runNow()

        assertThat(recorder.started).containsExactly("noticeDispatch")
    }

    private fun props(jobEnabled: Boolean) = SchedulerProperties(
        enabled = true,
        notification = SchedulerProperties.NotificationSchedulerProps(
            noticeDispatch = SchedulerProperties.FixedDelayJobProps(enabled = jobEnabled),
        ),
    )

    private class RecordingRecorder : SchedulerJobRunRecorder {
        val started = mutableListOf<String>()
        override fun start(jobName: String): Long? {
            started += jobName
            return null
        }
        override fun complete(runId: Long?, processedCount: Int?, totalCount: Int?) = Unit
        override fun fail(runId: Long?, throwable: Throwable) = Unit
        override fun skip(jobName: String, reason: String) = Unit
        override fun skipExisting(runId: Long?, reason: String) = Unit
    }
}
