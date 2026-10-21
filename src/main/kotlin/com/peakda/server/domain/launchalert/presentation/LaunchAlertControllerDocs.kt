package com.peakda.server.domain.launchalert.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.launchalert.presentation.request.SubscribeLaunchAlertRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Launch Alert", description = "앱 출시 알림 신청 API (랜딩 페이지)")
interface LaunchAlertControllerDocs {

    @Operation(
        summary = "앱 출시 알림 신청",
        description = "로그인 없이 이메일로 앱 출시 알림을 신청한다. 개인정보 수집·이용 동의(privacyConsent=true)가 필요하다. " +
            "응답은 201 이다. 이미 신청한 이메일이어도 동의 시각만 갱신하고 같은 응답을 준다. " +
            "같은 IP 의 신청 횟수는 일정 시간 단위로 제한된다(app.launch-alert.rate-limit).",
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.LAUNCH_ALERT_CONSENT_REQUIRED,
        ErrorCode.LAUNCH_ALERT_RATE_LIMIT,
    )
    @PostMapping
    fun subscribe(
        @Valid @RequestBody request: SubscribeLaunchAlertRequest,
        @Parameter(hidden = true)
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Unit>>
}
