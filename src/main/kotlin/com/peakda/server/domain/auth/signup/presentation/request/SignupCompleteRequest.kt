package com.peakda.server.domain.auth.signup.presentation.request

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SignupCompleteRequest(
    @field:Size(min = 2, max = 10)
    @field:Pattern(regexp = "^[가-힣a-zA-Z0-9]+$")
    val nickname: String,

    val profileImageUrl: String? = null,
)
