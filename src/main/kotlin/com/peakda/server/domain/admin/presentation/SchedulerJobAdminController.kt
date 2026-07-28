package com.peakda.server.domain.admin.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.admin.application.SchedulerJobAdminService
import com.peakda.server.domain.admin.presentation.response.SchedulerJobResponse
import com.peakda.server.domain.admin.presentation.response.SchedulerJobRunDetailResponse
import com.peakda.server.domain.admin.presentation.response.SchedulerJobRunResponse
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/admin/jobs")
class SchedulerJobAdminController(
    private val schedulerJobAdminService: SchedulerJobAdminService,
) : SchedulerJobAdminControllerDocs {

    override fun jobs(): ResponseEntity<ApiResponse<List<SchedulerJobResponse>>> {
        val response = schedulerJobAdminService.jobs()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun runs(
        jobName: String?,
        status: SchedulerJobStatus?,
        since: Instant?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<SchedulerJobRunResponse>>> {
        val response = schedulerJobAdminService
            .runs(jobName, status, since, pageRequest.toPageable())
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun run(id: Long): ResponseEntity<ApiResponse<SchedulerJobRunDetailResponse>> {
        val response = schedulerJobAdminService.run(id)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun trigger(
        principal: PrincipalDetails,
        jobName: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        val adminId = requireNotNull(principal.getUser().id)
        schedulerJobAdminService.trigger(adminId, jobName)
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(HttpStatus.ACCEPTED))
    }
}
