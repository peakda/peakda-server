package com.peakda.server.domain.launchalert.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertPurgeResponse
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertRecipientsResponse
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertSubscriptionResponse
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
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Admin Launch Alert", description = "백오피스 앱 출시 알림 신청 관리 API")
interface LaunchAlertAdminControllerDocs {

    @Operation(
        summary = "출시 알림 신청 목록 조회",
        description = "신청을 최신순으로 페이징 조회한다. 이메일 부분일치로 좁힐 수 있다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping
    fun list(
        @Parameter(description = "앱 플랫폼", example = "IOS")
        @RequestParam(name = "platform", defaultValue = "IOS") platform: LaunchAlertPlatform,
        @Parameter(description = "이메일 검색어 (부분일치)", example = "xxx.com")
        @RequestParam(name = "email", required = false) email: String?,
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<LaunchAlertSubscriptionResponse>>>

    @Operation(
        summary = "출시 안내 발송 대상 내려받기",
        description = "남아 있는 신청 이메일 전체를 신청 순으로 돌려주고, 돌려준 신청을 '내려받음'으로 표시한다. " +
            "내려받은 기록이 감사 로그에 남는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping("/recipients")
    fun recipients(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "앱 플랫폼", example = "IOS")
        @RequestParam(name = "platform", defaultValue = "IOS") platform: LaunchAlertPlatform,
    ): ResponseEntity<ApiResponse<LaunchAlertRecipientsResponse>>

    @Operation(
        summary = "발송 완료한 신청 파기",
        description = "출시 안내를 보낸 뒤 내려받은 신청을 지운다. 내려받은 뒤 들어온 신청은 남는다. 되돌릴 수 없다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @DeleteMapping
    fun purge(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "앱 플랫폼", example = "IOS")
        @RequestParam(name = "platform", defaultValue = "IOS") platform: LaunchAlertPlatform,
    ): ResponseEntity<ApiResponse<LaunchAlertPurgeResponse>>

    @Operation(
        summary = "출시 알림 신청 한 건 삭제",
        description = "신청자가 철회를 요청했을 때 한 건만 지운다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.LAUNCH_ALERT_SUBSCRIPTION_NOT_FOUND,
    )
    @DeleteMapping("/{id}")
    fun delete(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "신청 id", example = "101")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<Unit>>
}
