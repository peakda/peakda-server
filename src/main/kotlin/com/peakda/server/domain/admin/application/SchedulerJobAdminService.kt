package com.peakda.server.domain.admin.application

import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.admin.exception.SchedulerJobAlreadyRunningException
import com.peakda.server.domain.admin.exception.SchedulerJobNotFoundException
import com.peakda.server.domain.admin.presentation.response.SchedulerJobResponse
import com.peakda.server.domain.admin.presentation.response.SchedulerJobRunDetailResponse
import com.peakda.server.domain.admin.presentation.response.SchedulerJobRunResponse
import com.peakda.server.infrastructure.scheduler.ManualJobExecutor
import com.peakda.server.infrastructure.scheduler.ManualJobRegistry
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRepository
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SchedulerJobAdminService(
    private val schedulerJobRunRepository: SchedulerJobRunRepository,
    private val manualJobRegistry: ManualJobRegistry,
    private val manualJobExecutor: ManualJobExecutor,
    private val adminAuditRecorder: AdminAuditRecorder,
) {

    @Transactional(readOnly = true)
    fun jobs(): List<SchedulerJobResponse> {
        val latestRunsByJobName = schedulerJobRunRepository.findLatestRunPerJob()
            .associateBy { it.jobName }
        val names = (manualJobRegistry.names() + latestRunsByJobName.keys)
            .distinct()
            .sorted()
        return names.map { jobName ->
            val latestRun = latestRunsByJobName[jobName]
                ?.let(SchedulerJobRunResponse::summaryOf)
            SchedulerJobResponse(jobName = jobName, latestRun = latestRun)
        }
    }

    @Transactional(readOnly = true)
    fun runs(
        jobName: String?,
        status: SchedulerJobStatus?,
        since: Instant?,
        pageable: Pageable,
    ): Page<SchedulerJobRunResponse> {
        return schedulerJobRunRepository.findRuns(jobName, status, since, pageable)
            .map(SchedulerJobRunResponse::summaryOf)
    }

    @Transactional(readOnly = true)
    fun run(id: Long): SchedulerJobRunDetailResponse {
        val run = schedulerJobRunRepository.findById(id)
            .orElseThrow { SchedulerJobNotFoundException() }
        return SchedulerJobRunDetailResponse.of(run)
    }

    fun trigger(adminId: Long, jobName: String) {
        val job = manualJobRegistry.find(jobName) ?: throw SchedulerJobNotFoundException()
        if (schedulerJobRunRepository.existsByJobNameAndStatus(jobName, SchedulerJobStatus.RUNNING)) {
            throw SchedulerJobAlreadyRunningException()
        }
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = AdminAuditAction.SCHEDULER_JOB_TRIGGER,
                targetType = AdminAuditTargetType.SCHEDULER_JOB,
                targetId = 0,
                memo = jobName,
            ),
        )
        manualJobExecutor.execute(job)
    }
}
