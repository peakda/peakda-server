package com.peakda.server.domain.auth.app.presentation.request

import com.peakda.server.domain.auth.session.presentation.request.SessionDeviceRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "iOS Sign in with Apple 로그인 요청")
data class AppleLoginRequest(
    @field:NotBlank
    @field:Size(max = 8192)
    @field:Schema(description = "ASAuthorizationAppleIDCredential.identityToken (JWT)")
    val identityToken: String,

    @field:NotBlank
    @field:Size(max = 1024)
    @field:Schema(description = "ASAuthorizationAppleIDCredential.authorizationCode. 회원 탈퇴 때 애플 토큰을 폐기하는 데 쓴다")
    val authorizationCode: String,

    @field:NotBlank
    @field:Size(max = 256)
    @field:Schema(
        description = "앱이 만든 nonce 원문. 애플 요청에는 이 값의 SHA-256(hex)을 넣어야 한다",
        example = "4f3c2b1a9e8d7c6b5a4f3c2b1a9e8d7c",
    )
    val nonce: String,

    @field:Valid
    @field:Schema(description = "로그인 기기 정보. 구버전 앱은 생략할 수 있다.", nullable = true)
    val device: SessionDeviceRequest? = null,
)
