package com.peakda.server.domain.user.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.presentation.response.FollowSummaryResponse
import com.peakda.server.domain.user.presentation.response.FollowUserResponse
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

@Tag(name = "User Follow", description = "사용자 팔로우 API")
interface FollowControllerDocs {

    @Operation(
        summary = "사용자 팔로우",
        description = "대상 사용자를 팔로우한다. 이미 팔로우 중이면 그대로 성공으로 응답한다 (멱등). " +
            "자기 자신은 팔로우할 수 없다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.SELF_FOLLOW_NOT_ALLOWED,
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @PostMapping("/{userId}/follow")
    fun follow(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("userId") userId: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "사용자 언팔로우",
        description = "대상 사용자 팔로우를 해제한다. 팔로우하지 않은 사용자여도 성공으로 응답한다 (멱등).",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @DeleteMapping("/{userId}/follow")
    fun unfollow(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("userId") userId: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "팔로워 목록 조회",
        description = "대상 사용자를 팔로우하는 사람들을 최근 팔로우한 순으로 페이징 조회한다. " +
            "각 항목의 following 필드는 현재 로그인 사용자 기준의 팔로우 여부다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @GetMapping("/{userId}/followers")
    fun followers(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("userId") userId: Long,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>>

    @Operation(
        summary = "팔로잉 목록 조회",
        description = "대상 사용자가 팔로우하는 사람들을 최근 팔로우한 순으로 페이징 조회한다. " +
            "각 항목의 following 필드는 현재 로그인 사용자 기준의 팔로우 여부다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @GetMapping("/{userId}/followings")
    fun followings(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("userId") userId: Long,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>>

    @Operation(
        summary = "팔로우 통계 요약",
        description = "대상 사용자의 팔로워 수·팔로잉 수와, 현재 로그인 사용자의 팔로우 여부를 조회한다. " +
            "유저 프로필 헤더 표시에 사용한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.RESOURCE_NOT_FOUND)
    @GetMapping("/{userId}/follow-summary")
    fun summary(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @PathVariable("userId") userId: Long,
    ): ResponseEntity<ApiResponse<FollowSummaryResponse>>
}
