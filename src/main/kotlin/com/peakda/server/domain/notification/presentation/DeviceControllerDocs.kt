package com.peakda.server.domain.notification.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.notification.presentation.request.RegisterDeviceRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Device", description = "디바이스 토큰 API")
interface DeviceControllerDocs {

    @Operation(
        summary = "디바이스 토큰 등록",
        description = "푸시 알림용 디바이스 토큰을 멱등하게 등록한다. 이미 등록된 토큰이면 소유자와 플랫폼을 갱신한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @PostMapping
    fun register(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: RegisterDeviceRequest,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "디바이스 토큰 해제",
        description = "로그아웃 시 현재 사용자의 디바이스 토큰을 멱등하게 해제한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @DeleteMapping("/{token}")
    fun unregister(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "해제할 디바이스 토큰", example = "fcm-device-token-example")
        @PathVariable("token") token: String,
    ): ResponseEntity<ApiResponse<Unit>>
}
