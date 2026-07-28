package com.peakda.server.domain.report.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.report.application.ReportAdminService
import com.peakda.server.domain.report.entity.ReportStatus
import com.peakda.server.domain.report.entity.ReportTargetType
import com.peakda.server.domain.report.presentation.request.ReviewReportTargetRequest
import com.peakda.server.domain.report.presentation.response.ReportTargetDetailResponse
import com.peakda.server.domain.report.presentation.response.ReportTargetSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/reports")
class ReportAdminController(
    private val reportAdminService: ReportAdminService,
) : ReportAdminControllerDocs {

    override fun list(
        status: ReportStatus,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<ReportTargetSummaryResponse>>> {
        val response = reportAdminService.list(status, pageRequest.toPageable()).toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun detail(
        targetType: ReportTargetType,
        targetId: Long,
    ): ResponseEntity<ApiResponse<ReportTargetDetailResponse>> {
        val response = reportAdminService.detail(targetType, targetId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun review(
        principal: PrincipalDetails,
        targetType: ReportTargetType,
        targetId: Long,
        request: ReviewReportTargetRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        val adminId = requireNotNull(principal.getUser().id)
        reportAdminService.review(request.toCommand(adminId, targetType, targetId))
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
