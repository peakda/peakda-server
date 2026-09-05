package com.peakda.server.domain.admin.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "스케줄러 잡과 최근 실행 요약")
data class SchedulerJobResponse(
    @field:Schema(description = "잡 이름", example = "festivalSync")
    val jobName: String,

    @field:Schema(description = "최근 실행. 실행 이력이 없으면 null", nullable = true)
    val latestRun: SchedulerJobRunResponse?,
)
