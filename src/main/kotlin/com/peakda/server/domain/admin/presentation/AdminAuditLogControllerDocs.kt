package com.peakda.server.domain.admin.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.admin.presentation.response.AdminAuditLogResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Admin Audit Log", description = "백오피스 관리자 감사 로그 API")
interface AdminAuditLogControllerDocs {

    @Operation(
        summary = "관리자 감사 로그 목록 조회",
        description = "대상 종류와 id, 관리자 id 또는 전체 조건으로 최신 감사 로그를 페이징 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "조치 대상 종류", example = "CURATION")
        @RequestParam(name = "targetType", required = false) targetType: AdminAuditTargetType?,
        @Parameter(description = "조치 대상 id", example = "101")
        @RequestParam(name = "targetId", required = false) targetId: Long?,
        @Parameter(description = "조치를 수행한 관리자 사용자 id", example = "7")
        @RequestParam(name = "adminId", required = false) adminId: Long?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<AdminAuditLogResponse>>>
}
