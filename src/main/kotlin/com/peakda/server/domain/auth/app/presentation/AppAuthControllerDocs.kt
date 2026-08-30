package com.peakda.server.domain.auth.app.presentation

import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.openapi.ApiErrorResponses
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.auth.app.presentation.request.AppTokenExchangeRequest
import com.peakda.server.domain.auth.app.presentation.request.AppTokenRefreshRequest
import com.peakda.server.domain.auth.app.presentation.response.AppTokenResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "App Auth", description = "앱(Capacitor) 전용 인증 API")
interface AppAuthControllerDocs {

    @Operation(
        summary = "일회성 코드를 토큰으로 교환",
        description = "앱은 `/oauth2/authorization/{provider}?client=app` 으로 Custom Tab 인증을 시작한다. " +
            "인증이 끝나면 서버가 `peakda://auth/callback?code=...` 로 되돌려 보내고, 그 코드를 여기서 토큰으로 바꾼다. " +
            "코드는 한 번만 쓸 수 있고 발급 후 60초 안에 교환해야 한다.\n\n" +
            "가입이 끝난 사용자는 `status=AUTHENTICATED` 와 함께 액세스·리프레시 토큰을 받고, " +
            "회원가입이 남은 사용자는 `status=SIGNUP_REQUIRED` 와 함께 가입 세션 토큰을 받는다. " +
            "가입 세션 토큰은 `/api/auth/signup/*` 에 `Authorization: Bearer` 로 보낸다.",
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.AUTH_CODE_INVALID,
        ErrorCode.SIGNUP_SESSION_EXPIRED,
        ErrorCode.USER_NOT_FOUND,
        ErrorCode.UNAUTHORIZED,
    )
    @PostMapping("/token")
    fun exchange(
        @Valid @RequestBody request: AppTokenExchangeRequest,
    ): ResponseEntity<ApiResponse<AppTokenResponse>>

    @Operation(
        summary = "앱 토큰 재발급",
        description = "리프레시 토큰으로 새 액세스·리프레시 토큰을 받는다. 재발급 시 리프레시 토큰도 함께 회전하므로 " +
            "응답의 리프레시 토큰으로 교체해 두어야 한다. 쿠키를 쓰는 웹은 `/api/auth/refresh` 를 그대로 쓴다.",
    )
    @ApiErrorResponses(
        ErrorCode.INVALID_REQUEST,
        ErrorCode.REFRESH_TOKEN_EXPIRED,
        ErrorCode.REFRESH_TOKEN_INVALID,
        ErrorCode.RESOURCE_NOT_FOUND,
    )
    @PostMapping("/token/refresh")
    fun refresh(
        @Valid @RequestBody request: AppTokenRefreshRequest,
    ): ResponseEntity<ApiResponse<AppTokenResponse>>
}
