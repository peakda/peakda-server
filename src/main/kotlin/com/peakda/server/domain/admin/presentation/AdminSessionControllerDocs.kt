package com.peakda.server.domain.admin.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.admin.presentation.response.AdminSessionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "Admin Session", description = "백오피스 관리자 세션 API")
interface AdminSessionControllerDocs {

    @Operation(
        summary = "현재 관리자 세션 조회",
        description = "백오피스 셸이 현재 로그인 및 관리자 권한 여부를 확인한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.FORBIDDEN)
    @GetMapping
    fun getSession(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<AdminSessionResponse>>
}
