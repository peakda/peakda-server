package com.peakda.server.domain.auth.session.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.auth.session.presentation.response.DeviceSessionListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

@Tag(name = "Device Session", description = "로그인 기기 관리 API")
interface DeviceSessionControllerDocs {

    @Operation(
        summary = "로그인된 기기 목록",
        description = "현재 사용자 계정으로 로그인된 기기(앱 설치·웹 브라우저)를 등록일 최신순으로 조회한다. " +
            "최대 maxCount 대까지 로그인할 수 있고, 넘으면 가장 오래 사용하지 않은 기기가 자동으로 로그아웃된다. " +
            "current 가 true 인 항목이 지금 요청한 기기다.",
        security = [SecurityRequirement(name = "accessTokenCookie"), SecurityRequirement(name = "bearerToken")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<DeviceSessionListResponse>>

    @Operation(
        summary = "기기 해제",
        description = "선택한 기기를 로그아웃시킨다. 그 기기의 토큰은 즉시 사용할 수 없고 푸시 알림도 더 이상 가지 않는다. " +
            "지금 요청한 기기를 해제하면 이 기기도 로그아웃되므로, 클라이언트는 로그아웃 상태로 전환한다.",
        security = [SecurityRequirement(name = "accessTokenCookie"), SecurityRequirement(name = "bearerToken")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.DEVICE_SESSION_NOT_FOUND)
    @DeleteMapping("/{sessionId}")
    fun revoke(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "해제할 기기(세션) id", example = "3f2b8c1e-6a7d-4f0e-9c2a-1b5d8e7f9a10")
        @PathVariable("sessionId") sessionId: UUID,
    ): ResponseEntity<ApiResponse<Unit>>
}
