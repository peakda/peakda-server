package com.peakda.server.domain.user.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.presentation.response.BlockedUserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping

@Tag(name = "User Block", description = "사용자 차단 API")
interface BlockControllerDocs {

    @Operation(
        summary = "사용자 차단",
        description = "대상 사용자를 차단한다. 이미 차단 중이면 그대로 성공으로 응답한다 (멱등). " +
            "자기 자신은 차단할 수 없다. 차단 시 서로의 팔로우 관계도 함께 해제된다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SELF_BLOCK_NOT_ALLOWED,
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @PostMapping("/{userId}/block")
    fun block(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("userId") userId: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "사용자 차단 해제",
        description = "대상 사용자 차단을 해제한다. 차단하지 않은 사용자여도 성공으로 응답한다 (멱등).",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @DeleteMapping("/{userId}/block")
    fun unblock(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("userId") userId: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "차단한 사용자 목록",
        description = "본인이 차단한 사용자를 최근 차단한 순으로 페이징 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
    )
    @GetMapping("/me/blocks")
    fun list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<BlockedUserResponse>>>
}
