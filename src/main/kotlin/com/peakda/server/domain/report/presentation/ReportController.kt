package com.peakda.server.domain.report.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.report.application.ReportService
import com.peakda.server.domain.report.presentation.request.CreateReportRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService,
) : ReportControllerDocs {

    override fun create(
        principal: PrincipalDetails,
        request: CreateReportRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        reportService.create(userId, request.targetType, request.targetId, request.reason, request.detail)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
