package com.peakda.server.domain.notification.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.notification.entity.NotificationSegment
import com.peakda.server.domain.notification.presentation.response.NotificationResponse
import com.peakda.server.domain.notification.presentation.response.UnreadCountResponse
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
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Notification", description = "알림 API")
interface NotificationControllerDocs {

    @Operation(
        summary = "알림 목록 조회",
        description = "현재 로그인한 사용자의 알림을 세그먼트(탭)별로 최신순 조회한다. " +
            "all=전체, timing=만개, activity=팔로우·리액션, notice=공지.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.INVALID_REQUEST, ErrorCode.UNAUTHORIZED)
    @GetMapping
    fun list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "알림 세그먼트", example = "ALL")
        @RequestParam(name = "segment", defaultValue = "ALL") segment: NotificationSegment,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>>

    @Operation(
        summary = "안 읽은 알림 개수",
        description = "현재 로그인한 사용자의 안 읽은 알림 개수를 반환한다 (뱃지용).",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @GetMapping("/unread-count")
    fun unreadCount(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<UnreadCountResponse>>

    @Operation(
        summary = "알림 읽음 처리",
        description = "알림 1건을 읽음 처리한다. 본인 알림이 아니거나 존재하지 않으면 404.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED, ErrorCode.NOTIFICATION_NOT_FOUND)
    @PatchMapping("/{id}/read")
    fun markRead(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "알림 id", example = "9012")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "알림 전체 읽음 처리",
        description = "현재 로그인한 사용자의 안 읽은 알림을 모두 읽음 처리한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(ErrorCode.UNAUTHORIZED)
    @PatchMapping("/read-all")
    fun markAllRead(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<Unit>>
}
