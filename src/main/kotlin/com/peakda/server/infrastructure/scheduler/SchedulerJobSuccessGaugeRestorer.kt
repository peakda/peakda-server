package com.peakda.server.infrastructure.scheduler

import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 기동 시 [SchedulerJobSuccessGauge] 를 실행 이력으로 되살린다.
 *
 * 게이지를 되살리지 않으면 배포 직후부터 각 잡의 다음 성공까지 — 일 1회 잡이면 최대 26시간 —
 * 시계열이 비어 미실행 알림이 NoData 로 빠진다. develop 은 배포가 잦아 실제로 자주 발생한다.
 * 잡 실행 이력은 `scheduler_job_runs` 에 이미 영속되어 있으므로 그것을 원본으로 삼는다.
 */
@Component
class SchedulerJobSuccessGaugeRestorer(
    private val gauge: SchedulerJobSuccessGauge,
    private val repository: SchedulerJobRunRepository,
) {
    /**
     * 관측 보조 기능이 앱 기동을 막으면 안 되므로 실패해도 삼킨다.
     * 복원에 실패하면 다음 성공 때 게이지가 다시 생긴다.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun restore() {
        try {
            val restored = repository.findLastSuccessRunPerJob()
                .mapNotNull { run -> run.finishedAt?.let { run.jobName to it } }
            restored.forEach { (jobName, finishedAt) -> gauge.record(jobName, finishedAt) }
            log.info("[scheduler] last_success_timestamp 복원 jobs={}", restored.size)
        } catch (e: Exception) {
            log.warn("[scheduler] last_success_timestamp 복원 실패. 다음 성공까지 시계열이 비어 있다", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SchedulerJobSuccessGaugeRestorer::class.java)
    }
}
