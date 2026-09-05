package com.peakda.server.domain.auth.signup.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "닉네임 사용 가능 여부 응답")
data class NicknameCheckResponse(
    @field:Schema(description = "true: 사용 가능, false: 이미 사용 중", example = "true")
    val available: Boolean,
)
