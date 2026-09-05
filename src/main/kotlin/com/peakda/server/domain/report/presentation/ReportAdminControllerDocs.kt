package com.peakda.server.domain.report.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.report.entity.ReportStatus
import com.peakda.server.domain.report.entity.ReportTargetType
import com.peakda.server.domain.report.presentation.request.ReviewReportTargetRequest
import com.peakda.server.domain.report.presentation.response.ReportTargetDetailResponse
import com.peakda.server.domain.report.presentation.response.ReportTargetSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Report Admin", description = "신고 심사 관리자 API")
interface ReportAdminControllerDocs {

    @Operation(
        summary = "신고 대상별 심사 목록 조회",
        description = "심사 상태별로 신고를 대상 단위로 집계해 신고 건수 순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "신고 심사 상태", example = "PENDING")
        @RequestParam(name = "status", defaultValue = "PENDING") status: ReportStatus,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<ReportTargetSummaryResponse>>>

    @Operation(
        summary = "신고 대상 심사 상세 조회",
        description = "사유 분포, 개별 신고 목록, 삭제 여부를 포함한 대상 콘텐츠 요약을 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.REPORT_NOT_FOUND,
    )
    @GetMapping("/targets/{targetType}/{targetId}")
    fun detail(
        @Parameter(description = "신고 대상 종류", example = "SPOT_RECORD")
        @PathVariable("targetType") targetType: ReportTargetType,
        @Parameter(description = "신고 대상 id", example = "1024")
        @PathVariable("targetId") targetId: Long,
    ): ResponseEntity<ApiResponse<ReportTargetDetailResponse>>

    @Operation(
        summary = "신고 대상 일괄 심사",
        description = "같은 대상의 PENDING 신고를 한 번에 처리하며, 게시글 대상은 숨김 조치를 함께 적용할 수 있다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.REPORT_NOT_FOUND,
        ErrorCode.REPORT_ALREADY_REVIEWED,
        ErrorCode.REPORT_ACTION_NOT_SUPPORTED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
    )
    @PatchMapping("/targets/{targetType}/{targetId}")
    fun review(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "신고 대상 종류", example = "SPOT_RECORD")
        @PathVariable("targetType") targetType: ReportTargetType,
        @Parameter(description = "신고 대상 id", example = "1024")
        @PathVariable("targetId") targetId: Long,
        @Valid @RequestBody request: ReviewReportTargetRequest,
    ): ResponseEntity<ApiResponse<Unit>>
}
