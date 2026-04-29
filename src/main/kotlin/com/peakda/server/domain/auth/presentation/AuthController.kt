package com.peakda.server.domain.auth.presentation

import com.peakda.server.domain.auth.application.AuthService
import com.peakda.server.domain.auth.application.response.UserInfoResponse
import com.peakda.server.domain.auth.signup.presentation.response.NicknameCheckResponse
import com.peakda.server.domain.auth.signup.presentation.request.SignupCompleteRequest
import com.peakda.server.global.model.ApiResponse
import com.peakda.server.global.security.principal.PrincipalDetails
import com.peakda.server.global.security.principal.SignupSessionPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Auth", description = "로그인, 회원가입, 토큰 관리 API")
class AuthController(
    private val authService: AuthService
) {

    @Operation(
        summary = "내 정보 조회",
        description = "access-token 쿠키로 현재 로그인한 사용자의 정보를 조회합니다.",
        security = [SecurityRequirement(name = "accessTokenCookie")]
    )
    @GetMapping("/me")
    fun getCurrentUser(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails
    ): ResponseEntity<ApiResponse<UserInfoResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val userInfo = authService.getUserInfo(userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, userInfo))
    }

    @Operation(
        summary = "회원가입 닉네임 중복 확인",
        description = "소셜 로그인 후 발급된 signup-token 쿠키로 닉네임 사용 가능 여부를 확인합니다.",
        security = [SecurityRequirement(name = "signupTokenCookie")]
    )
    @GetMapping("/signup/nickname/check")
    fun checkNickname(
        @Parameter(description = "2~10자의 한글, 영문, 숫자 닉네임", example = "peakda")
        @RequestParam value: String,
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: SignupSessionPrincipal,
    ): ResponseEntity<ApiResponse<NicknameCheckResponse>> {
        val result = authService.checkNickname(value)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result))
    }

    @Operation(
        summary = "소셜 회원가입 완료",
        description = "signup-token 쿠키와 닉네임으로 회원가입을 완료하고 access-token, refresh-token 쿠키를 발급합니다.",
        security = [SecurityRequirement(name = "signupTokenCookie")]
    )
    @PostMapping("/signup/complete")
    fun completeSignup(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: SignupSessionPrincipal,
        @Valid @RequestBody request: SignupCompleteRequest,
        @Parameter(hidden = true)
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.completeSignup(principal, request, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    @Operation(
        summary = "토큰 재발급",
        description = "refresh-token 쿠키로 access-token, refresh-token 쿠키를 재발급합니다.",
        security = [SecurityRequirement(name = "refreshTokenCookie")]
    )
    @PostMapping("/refresh")
    fun refresh(
        @Parameter(hidden = true)
        request: HttpServletRequest,
        @Parameter(hidden = true)
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.refresh(request, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    @Operation(
        summary = "로그아웃",
        description = "서버에 저장된 refresh token을 삭제하고 access-token, refresh-token 쿠키를 만료시킵니다.",
        security = [SecurityRequirement(name = "accessTokenCookie")]
    )
    @PostMapping("/logout")
    fun logout(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: PrincipalDetails,
        @Parameter(hidden = true)
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        authService.logout(userId, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
