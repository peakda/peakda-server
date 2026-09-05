package com.peakda.server.domain.admin.application

import com.peakda.server.domain.admin.exception.SchedulerJobNotFoundException
import com.peakda.server.domain.admin.presentation.response.SchedulerJobRunDetailResponse
import com.peakda.server.infrastructure.scheduler.ManualJobExecutor
import com.peakda.server.infrastructure.scheduler.ManualJobRegistry
import com.peakda.server.infrastructure.scheduler.ManualTriggerableJob
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRun
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRepository
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class SchedulerJobAdminServiceTest {

    private val repository = mock(SchedulerJobRunRepository::class.java)
    private val registry = ManualJobRegistry(listOf(job("alpha"), job("beta")))
    private val executor = mock(ManualJobExecutor::class.java)
    private val auditRecorder = mock(AdminAuditRecorder::class.java)
    private val service = SchedulerJobAdminService(repository, registry, executor, auditRecorder)

    @Test
    fun `잡 목록은 최근 실행을 한 번에 조회한다`() {
        `when`(repository.findLatestRunPerJob()).thenReturn(emptyList())

        service.jobs()

        verify(repository, times(1)).findLatestRunPerJob()
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `실행 이력은 since 를 포함한 통합 조건 쿼리로 조회한다`() {
        val pageable = PageRequest.of(0, 20)
        val since = Instant.parse("2026-07-27T00:00:00Z")
        val run = schedulerRun(id = 10L, errorMessage = "boom", errorStack = "stacktrace")
        `when`(repository.findRuns("alpha", SchedulerJobStatus.FAILED, since, pageable))
            .thenReturn(PageImpl(listOf(run), pageable, 1))

        val response = service.runs("alpha", SchedulerJobStatus.FAILED, since, pageable)

        verify(repository, times(1)).findRuns("alpha", SchedulerJobStatus.FAILED, since, pageable)
        assertThat(response.content.single()::class.java.declaredFields.map { it.name })
            .doesNotContain("errorStack")
    }

    @Test
    fun `실행 상세는 별도 응답 타입으로 오류 스택을 포함한다`() {
        val run = schedulerRun(id = 10L, errorMessage = "boom", errorStack = "stacktrace")
        `when`(repository.findById(10L)).thenReturn(Optional.of(run))

        val response = service.run(10L)

        assertThat(response).isInstanceOf(SchedulerJobRunDetailResponse::class.java)
        assertThat(response.errorMessage).isEqualTo("boom")
        assertThat(response.errorStack).isEqualTo("stacktrace")
    }

    @Test
    fun `존재하지 않는 실행 상세는 스케줄러 잡 없음 예외를 던진다`() {
        `when`(repository.findById(404L)).thenReturn(Optional.empty())

        assertThatThrownBy { service.run(404L) }
            .isInstanceOf(SchedulerJobNotFoundException::class.java)
    }

    private fun job(name: String) = object : ManualTriggerableJob {
        override val jobName = name

        override fun runNow() = Unit
    }

    private fun schedulerRun(
        id: Long,
        errorMessage: String?,
        errorStack: String?,
    ): SchedulerJobRun {
        val run = SchedulerJobRun(
            jobName = "alpha",
            startedAt = Instant.parse("2026-07-28T00:00:00Z"),
        )
        run.fail(Instant.parse("2026-07-28T00:01:00Z"), errorMessage, errorStack)
        ReflectionTestUtils.setField(run, "id", id)
        return run
    }
}
