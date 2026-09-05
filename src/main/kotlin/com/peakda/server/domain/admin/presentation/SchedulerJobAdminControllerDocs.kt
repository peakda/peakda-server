package com.peakda.server.domain.admin.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.admin.presentation.response.SchedulerJobResponse
import com.peakda.server.domain.admin.presentation.response.SchedulerJobRunDetailResponse
import com.peakda.server.domain.admin.presentation.response.SchedulerJobRunResponse
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.Instant

@Tag(name = "Scheduler Job Admin", description = "스케줄러 잡 조회 및 수동 실행 관리자 API")
interface SchedulerJobAdminControllerDocs {

    @Operation(
        summary = "스케줄러 잡 목록 조회",
        description = "등록된 수동 실행 대상과 과거 이력에 존재하는 잡을 최근 실행 요약과 함께 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun jobs(): ResponseEntity<ApiResponse<List<SchedulerJobResponse>>>

    @Operation(
        summary = "스케줄러 잡 실행 이력 조회",
        description = "잡 이름, 상태, 시작 시각 하한을 선택적으로 필터링해 최신 이력부터 페이징 조회한다. 오류 스택은 목록에 포함하지 않는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping("/runs")
    fun runs(
        @Parameter(description = "잡 이름", example = "festivalSync")
        @RequestParam(name = "jobName", required = false) jobName: String?,
        @Parameter(description = "실행 상태", example = "FAILED")
        @RequestParam(name = "status", required = false) status: SchedulerJobStatus?,
        @Parameter(description = "시작 시각 하한", example = "2026-07-27T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(name = "since", required = false) since: Instant?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SchedulerJobRunResponse>>>

    @Operation(
        summary = "스케줄러 잡 실행 상세 조회",
        description = "단일 실행 이력을 조회한다. 실패 스택은 상세 응답에서만 포함한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.SCHEDULER_JOB_NOT_FOUND,
    )
    @GetMapping("/runs/{id}")
    fun run(
        @Parameter(description = "실행 이력 id", example = "1001")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<SchedulerJobRunDetailResponse>>

    @Operation(
        summary = "스케줄러 잡 수동 실행",
        description = "잡을 비동기로 실행하고 즉시 202를 반환한다. 실행 결과는 잡 이력에서 확인한다. " +
            "enabled 설정은 크론 자동 실행만 제어하므로 비활성 잡도 수동으로는 실행되며 실행 이력이 남는다. " +
            "같은 잡이 이미 실행 중이면 409로 거절한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.SCHEDULER_JOB_NOT_FOUND,
        ErrorCode.SCHEDULER_JOB_ALREADY_RUNNING,
    )
    @PostMapping("/{jobName}/trigger")
    fun trigger(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "잡 이름", example = "festivalSync")
        @PathVariable("jobName") jobName: String,
    ): ResponseEntity<ApiResponse<Unit>>
}
