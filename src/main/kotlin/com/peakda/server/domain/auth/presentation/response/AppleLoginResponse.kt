package com.peakda.server.domain.auth.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Apple 로그인 결과")
data class AppleLoginResponse(
    @field:Schema(
        description = "추가 회원가입이 필요한지 여부. " +
            "true 면 signup-token 쿠키가 발급되어 회원가입 완료(/api/auth/signup/complete)가 필요하고, " +
            "false 면 access-token·refresh-token 쿠키가 발급되어 로그인이 완료된 상태다.",
        example = "false",
    )
    val signupRequired: Boolean,
)
