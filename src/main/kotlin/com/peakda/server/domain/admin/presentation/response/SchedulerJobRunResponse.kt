package com.peakda.server.domain.admin.presentation.response

import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRun
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "스케줄러 잡 실행 이력")
data class SchedulerJobRunResponse(
    @field:Schema(description = "실행 이력 id", example = "1001")
    val id: Long,

    @field:Schema(description = "잡 이름", example = "festivalSync")
    val jobName: String,

    @field:Schema(description = "실행 시작 시각", example = "2026-07-28T09:30:00Z")
    val startedAt: Instant,

    @field:Schema(description = "실행 종료 시각", example = "2026-07-28T09:31:12Z", nullable = true)
    val finishedAt: Instant?,

    @field:Schema(description = "실행 상태", example = "COMPLETED")
    val status: SchedulerJobStatus,

    @field:Schema(description = "처리 건수", example = "320", nullable = true)
    val processedCount: Int?,

    @field:Schema(description = "전체 건수", example = "350", nullable = true)
    val totalCount: Int?,

    @field:Schema(description = "실패 메시지", example = "외부 API 응답 시간이 초과되었습니다.", nullable = true)
    val errorMessage: String?,

    @field:Schema(description = "건너뛴 사유", example = "locked", nullable = true)
    val skipReason: String?,
) {
    companion object {
        fun summaryOf(run: SchedulerJobRun): SchedulerJobRunResponse = SchedulerJobRunResponse(
            id = requireNotNull(run.id),
            jobName = run.jobName,
            startedAt = run.startedAt,
            finishedAt = run.finishedAt,
            status = run.status,
            processedCount = run.processedCount,
            totalCount = run.totalCount,
            errorMessage = run.errorMessage,
            skipReason = run.skipReason,
        )
    }
}
