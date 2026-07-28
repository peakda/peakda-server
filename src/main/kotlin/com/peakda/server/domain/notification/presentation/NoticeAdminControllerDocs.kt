package com.peakda.server.domain.notification.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.notification.entity.NoticeStatus
import com.peakda.server.domain.notification.presentation.request.UpsertNoticeRequest
import com.peakda.server.domain.notification.presentation.response.NoticeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Notice Admin", description = "공지 작성 및 발송 관리자 API")
interface NoticeAdminControllerDocs {

    @Operation(
        summary = "공지 생성",
        description = "발송 전 수정 가능한 DRAFT 상태의 공지를 생성한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @PostMapping
    fun create(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: UpsertNoticeRequest,
    ): ResponseEntity<ApiResponse<NoticeResponse>>

    @Operation(
        summary = "공지 목록 조회",
        description = "공지 상태를 선택적으로 필터링해 최신순으로 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "공지 발송 상태", example = "DRAFT")
        @RequestParam(name = "status", required = false) status: NoticeStatus?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<NoticeResponse>>>

    @Operation(
        summary = "공지 상세 조회",
        description = "공지 원본과 현재 발송 상태 및 발송 수를 조회한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.NOTICE_NOT_FOUND,
    )
    @GetMapping("/{id}")
    fun get(
        @Parameter(description = "공지 id", example = "12")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<NoticeResponse>>

    @Operation(
        summary = "공지 수정",
        description = "DRAFT 상태의 공지만 수정한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.NOTICE_NOT_FOUND,
        ErrorCode.NOTICE_NOT_EDITABLE,
    )
    @PutMapping("/{id}")
    fun update(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "공지 id", example = "12")
        @PathVariable("id") id: Long,
        @Valid @RequestBody request: UpsertNoticeRequest,
    ): ResponseEntity<ApiResponse<NoticeResponse>>

    @Operation(
        summary = "공지 발송 시작",
        description = "DRAFT 공지를 DISPATCHING 상태로 전환하고 즉시 응답한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.NOTICE_NOT_FOUND,
        ErrorCode.NOTICE_ALREADY_DISPATCHED,
    )
    @PostMapping("/{id}/dispatch")
    fun dispatch(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "공지 id", example = "12")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<NoticeResponse>>

    @Operation(
        summary = "공지 취소",
        description = "DRAFT 공지를 CANCELED 상태로 전환한다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.NOTICE_NOT_FOUND,
        ErrorCode.NOTICE_NOT_EDITABLE,
    )
    @PostMapping("/{id}/cancel")
    fun cancel(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "공지 id", example = "12")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<NoticeResponse>>
}
