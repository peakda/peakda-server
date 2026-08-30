package com.peakda.server.domain.auth.app.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.peakda.server.common.security.jwt.TokenResponse
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "앱 인증 토큰. status 에 따라 채워지는 필드가 다르다.")
data class AppTokenResponse(
    @field:Schema(description = "인증 상태", example = "AUTHENTICATED")
    val status: AppAuthStatus,

    @field:Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String? = null,

    @field:Schema(description = "액세스 토큰. Authorization: Bearer 로 보낸다.")
    val accessToken: String? = null,

    @field:Schema(description = "리프레시 토큰")
    val refreshToken: String? = null,

    @field:Schema(description = "액세스 토큰 유효 시간(초)", example = "1800")
    val accessTokenExpiresIn: Long? = null,

    @field:Schema(description = "리프레시 토큰 유효 시간(초)", example = "604800")
    val refreshTokenExpiresIn: Long? = null,

    @field:Schema(description = "가입 세션 토큰. 회원가입 API 에 Authorization: Bearer 로 보낸다.")
    val signupToken: String? = null,

    @field:Schema(description = "가입 세션 토큰 남은 시간(초)", example = "900")
    val signupTokenExpiresIn: Long? = null,
) {
    companion object {
        fun authenticated(
            tokens: TokenResponse,
            accessTokenExpiresIn: Long,
            refreshTokenExpiresIn: Long,
        ): AppTokenResponse = AppTokenResponse(
            status = AppAuthStatus.AUTHENTICATED,
            tokenType = tokens.tokenType,
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            accessTokenExpiresIn = accessTokenExpiresIn,
            refreshTokenExpiresIn = refreshTokenExpiresIn,
        )

        fun signupRequired(signupToken: String, signupTokenExpiresIn: Long): AppTokenResponse = AppTokenResponse(
            status = AppAuthStatus.SIGNUP_REQUIRED,
            signupToken = signupToken,
            signupTokenExpiresIn = signupTokenExpiresIn,
        )
    }
}
