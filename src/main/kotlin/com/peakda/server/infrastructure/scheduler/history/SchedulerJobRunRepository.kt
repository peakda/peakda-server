package com.peakda.server.infrastructure.scheduler.history

import org.springframework.data.jpa.repository.JpaRepository

interface SchedulerJobRunRepository : JpaRepository<SchedulerJobRun, Long> {
    fun findTop20ByJobNameOrderByStartedAtDesc(jobName: String): List<SchedulerJobRun>
}
