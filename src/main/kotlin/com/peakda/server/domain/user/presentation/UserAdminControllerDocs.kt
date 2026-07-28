package com.peakda.server.domain.user.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.presentation.request.ChangeUserStatusRequest
import com.peakda.server.domain.user.presentation.response.UserAdminResponse
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

@Tag(name = "User Admin", description = "사용자 검색 및 제재 관리자 API")
interface UserAdminControllerDocs {

    @Operation(
        summary = "관리자 사용자 목록 조회",
        description = "닉네임 부분일치와 상태·역할 선택 조건을 조합해 최신 가입순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "닉네임 부분일치 검색어", example = "여행")
        @RequestParam(name = "q", required = false) q: String?,
        @Parameter(description = "사용자 상태", example = "SUSPENDED")
        @RequestParam(name = "status", required = false) status: UserStatus?,
        @Parameter(description = "사용자 역할. 조회 필터 전용", example = "USER")
        @RequestParam(name = "role", required = false) role: UserRole?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<UserAdminResponse>>>

    @Operation(
        summary = "사용자 제재 상태 변경",
        description = "ACTIVE와 SUSPENDED 사이에서만 상태를 변경한다. 역할 변경은 지원하지 않는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.USER_NOT_FOUND,
        ErrorCode.USER_STATUS_NOT_CHANGEABLE,
        ErrorCode.ADMIN_SELF_ACTION_NOT_ALLOWED,
    )
    @PatchMapping("/{id}/status")
    fun changeStatus(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "대상 사용자 id", example = "31")
        @PathVariable("id") id: Long,
        @Valid @RequestBody request: ChangeUserStatusRequest,
    ): ResponseEntity<ApiResponse<UserAdminResponse>>
}
