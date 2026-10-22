package com.peakda.server.domain.launchalert.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.presentation.request.RequestLaunchAlertDispatchRequest
import com.peakda.server.domain.launchalert.presentation.request.SendLaunchAlertTestMailRequest
import com.peakda.server.domain.launchalert.presentation.request.UpdateLaunchAlertMailTemplateRequest
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertMailTemplateResponse
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertDispatchResponse
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
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

    @Operation(
        summary = "출시 안내 발송 이력 조회",
        description = "발송 요청을 최신순으로 조회한다. 보내는 중인 발송의 진행 수도 여기서 본다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
    )
    @GetMapping("/dispatches")
    fun dispatches(
        @Valid @ModelAttribute pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<LaunchAlertDispatchResponse>>>

    @Operation(
        summary = "출시 안내 메일 발송 요청",
        description = "남은 신청 전체에 출시 안내 메일을 보내도록 요청한다. 스케줄러 잡이 30초 안에 시작해 " +
            "100건씩 보내고, 보낸 신청은 바로 지운다. 보내지 못한 신청은 목록에 남는다. 되돌릴 수 없다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.LAUNCH_ALERT_NO_RECIPIENTS,
        ErrorCode.LAUNCH_ALERT_DISPATCH_IN_PROGRESS,
        ErrorCode.LAUNCH_ALERT_MAIL_UNAVAILABLE,
    )
    @PostMapping("/dispatches")
    fun requestDispatch(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: RequestLaunchAlertDispatchRequest,
    ): ResponseEntity<ApiResponse<LaunchAlertDispatchResponse>>

    @Operation(
        summary = "출시 안내 발송 멈춤",
        description = "보내는 중인 발송을 멈춘다. 지금 보내고 있는 한 통까지만 나가고, 남은 신청은 그대로 남는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.LAUNCH_ALERT_DISPATCH_NOT_FOUND,
        ErrorCode.LAUNCH_ALERT_DISPATCH_NOT_CANCELABLE,
    )
    @PostMapping("/dispatches/{id}/cancel")
    fun cancelDispatch(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(description = "발송 id", example = "3")
        @PathVariable("id") id: Long,
    ): ResponseEntity<ApiResponse<LaunchAlertDispatchResponse>>

    @Operation(
        summary = "출시 안내 테스트 메일 발송",
        description = "전체 발송 전에 지정한 주소로 같은 메일을 한 통 보낸다. 신청 목록은 바뀌지 않는다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.LAUNCH_ALERT_MAIL_UNAVAILABLE,
        ErrorCode.LAUNCH_ALERT_MAIL_SEND_FAILED,
    )
    @PostMapping("/dispatches/test")
    fun sendTestMail(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: SendLaunchAlertTestMailRequest,
    ): ResponseEntity<ApiResponse<Unit>>

    @Operation(
        summary = "출시 안내 메일 문구 조회",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.LAUNCH_ALERT_MAIL_TEMPLATE_NOT_FOUND,
    )
    @GetMapping("/mail-template")
    fun mailTemplate(
        @Parameter(description = "앱 플랫폼", example = "IOS")
        @RequestParam(name = "platform", defaultValue = "IOS") platform: LaunchAlertPlatform,
    ): ResponseEntity<ApiResponse<LaunchAlertMailTemplateResponse>>

    @Operation(
        summary = "출시 안내 메일 문구 수정",
        description = "제목·큰 제목·본문·버튼 문구·하단 안내를 고친다. HTML 은 쓸 수 없고 그대로 글자로 나간다. " +
            "보내는 중에는 고칠 수 없다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.LAUNCH_ALERT_MAIL_TEMPLATE_NOT_FOUND,
        ErrorCode.LAUNCH_ALERT_DISPATCH_IN_PROGRESS,
    )
    @PutMapping("/mail-template")
    fun updateMailTemplate(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Valid @RequestBody request: UpdateLaunchAlertMailTemplateRequest,
    ): ResponseEntity<ApiResponse<LaunchAlertMailTemplateResponse>>

    @Operation(
        summary = "출시 안내 메일 미리보기",
        description = "저장된 문구로 실제 메일과 같은 HTML 을 돌려준다. 백오피스가 iframe 으로 띄운다. " +
            "storeUrl 이 없거나 App Store 주소가 아니면 기본 주소를 버튼에 건다.",
        security = [SecurityRequirement(name = "accessTokenCookie")],
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.UNAUTHORIZED,
        ErrorCode.FORBIDDEN,
        ErrorCode.LAUNCH_ALERT_MAIL_TEMPLATE_NOT_FOUND,
    )
    // produces 를 두지 않는다. 두면 오류 응답(JSON 봉투)을 text/html 로 쓰지 못해 400·404 가 500 으로 바뀐다.
    // 성공 응답의 Content-Type 은 컨트롤러가 정한다.
    @GetMapping("/mail-template/preview")
    fun previewMailTemplate(
        @Parameter(description = "앱 플랫폼", example = "IOS")
        @RequestParam(name = "platform", defaultValue = "IOS") platform: LaunchAlertPlatform,
        @Parameter(description = "버튼에 걸 App Store 주소", example = "https://apps.apple.com/kr/app/id0000000000")
        @RequestParam(name = "storeUrl", required = false) storeUrl: String?,
    ): ResponseEntity<String>
}
