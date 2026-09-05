package com.peakda.server.domain.report.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.report.presentation.request.CreateReportRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Report", description = "UGC 신고 API")
interface ReportControllerDocs {

    @Operation(
        summary = "게시글 신고",
        description = "게시된 스팟 기록을 신고한다. 같은 대상을 이미 신고했으면 그대로 성공으로 응답한다 (멱등). " +
            "본인 게시글은 신고할 수 없고, DRAFT 이거나 존재하지 않으면 404. 관리자 심사 처리는 별도 운영 도구에서 이뤄진다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SELF_REPORT_NOT_ALLOWED,
        ErrorCode.SPOT_RECORD_NOT_FOUND,
    )
    @PostMapping
    fun create(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: CreateReportRequest,
    ): ResponseEntity<ApiResponse<Unit>>
}
