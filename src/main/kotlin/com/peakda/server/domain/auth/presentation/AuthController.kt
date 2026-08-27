package com.peakda.server.domain.auth.presentation

import com.peakda.server.common.response.ApiResponse
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.common.security.principal.SignupSessionPrincipal
import com.peakda.server.domain.auth.application.AppleLoginService
import com.peakda.server.domain.auth.application.AuthService
import com.peakda.server.domain.auth.presentation.request.AppleLoginRequest
import com.peakda.server.domain.auth.presentation.response.AppleLoginResponse
import com.peakda.server.domain.auth.presentation.response.UserInfoResponse
import com.peakda.server.domain.auth.signup.presentation.request.SignupCompleteRequest
import com.peakda.server.domain.auth.signup.presentation.response.NicknameCheckResponse
import com.peakda.server.domain.user.presentation.response.ProfileImageResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val appleLoginService: AppleLoginService,
) : AuthControllerDocs {

    override fun appleLogin(
        request: AppleLoginRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<AppleLoginResponse>> {
        val result = appleLoginService.login(request.identityToken, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result))
    }

    override fun getCurrentUser(
        principal: PrincipalDetails,
    ): ResponseEntity<ApiResponse<UserInfoResponse>> {
        val userId = requireNotNull(principal.getUser().id)
        val userInfo = authService.getUserInfo(userId)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, userInfo))
    }

    override fun checkNickname(
        value: String,
        principal: SignupSessionPrincipal,
    ): ResponseEntity<ApiResponse<NicknameCheckResponse>> {
        val result = authService.checkNickname(value)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, result))
    }

    override fun uploadSignupProfileImage(
        principal: SignupSessionPrincipal,
        image: MultipartFile,
    ): ResponseEntity<ApiResponse<ProfileImageResponse>> {
        val response = authService.uploadSignupProfileImage(principal, image)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }

    override fun completeSignup(
        principal: SignupSessionPrincipal,
        request: SignupCompleteRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.completeSignup(principal, request, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.refresh(request, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }

    override fun logout(
        principal: PrincipalDetails,
        response: HttpServletResponse,
    ): ResponseEntity<ApiResponse<Unit>> {
        val userId = requireNotNull(principal.getUser().id)
        authService.logout(userId, response)
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK))
    }
}
