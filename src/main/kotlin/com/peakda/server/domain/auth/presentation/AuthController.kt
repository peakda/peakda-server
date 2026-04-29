package com.peakda.server.domain.auth.presentation

import com.peakda.server.domain.auth.application.AuthService
import com.peakda.server.domain.auth.application.response.UserInfoResponse
import com.peakda.server.domain.auth.signup.presentation.response.NicknameCheckResponse
import com.peakda.server.domain.auth.signup.presentation.request.SignupCompleteRequest
import com.peakda.server.global.model.ApiResponse
import com.peakda.server.global.security.principal.PrincipalDetails
import com.peakda.server.global.security.principal.SignupSessionPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal principal: PrincipalDetails
    ): ResponseEntity<ApiResponse<UserInfoResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val userInfo = authService.getUserInfo(userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, userInfo))
    }

    @GetMapping("/signup/nickname/check")
    fun checkNickname(
        @RequestParam value: String,
        @AuthenticationPrincipal principal: SignupSessionPrincipal,
    ): ResponseEntity<ApiResponse<NicknameCheckResponse>> {
        val result = authService.checkNickname(value)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result))
    }

    @PostMapping("/signup/complete")
    fun completeSignup(
        @AuthenticationPrincipal principal: SignupSessionPrincipal,
        @Valid @RequestBody request: SignupCompleteRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.completeSignup(principal, request, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.refresh(request, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal principal: PrincipalDetails,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        authService.logout(userId, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
