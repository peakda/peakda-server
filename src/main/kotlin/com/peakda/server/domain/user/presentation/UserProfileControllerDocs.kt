package com.peakda.server.domain.user.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.user.presentation.response.UserProfileResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "User Profile", description = "타인 프로필 조회 API")
interface UserProfileControllerDocs {

    @Operation(
        summary = "사용자 프로필 조회",
        description = "대상 사용자의 프로필(SCR-024h/i)을 조회한다. 통계(게시 기록 수·팔로워 수·팔로잉 수), " +
            "관심 꽃 카테고리(읽기전용), 최근 게시 기록 그리드 미리보기(상위 6건), " +
            "현재 로그인 사용자 기준 팔로우 상태를 함께 반환한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @GetMapping("/{id}")
    fun getProfile(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "대상 사용자 id", example = "42")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<UserProfileResponse>>
}
