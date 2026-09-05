package com.peakda.server.domain.auth.app.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.auth.app.application.AppAuthService
import com.peakda.server.domain.auth.app.presentation.request.AppTokenExchangeRequest
import com.peakda.server.domain.auth.app.presentation.request.AppTokenRefreshRequest
import com.peakda.server.domain.auth.app.presentation.response.AppTokenResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/app")
class AppAuthController(
    private val appAuthService: AppAuthService,
) : AppAuthControllerDocs {

    override fun exchange(
        request: AppTokenExchangeRequest,
    ): ResponseEntity<ApiResponse<AppTokenResponse>> {
        val response = appAuthService.exchange(request.code)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun refresh(
        request: AppTokenRefreshRequest,
    ): ResponseEntity<ApiResponse<AppTokenResponse>> {
        val response = appAuthService.refresh(request.refreshToken)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
