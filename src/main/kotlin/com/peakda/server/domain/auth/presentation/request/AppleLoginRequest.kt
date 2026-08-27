package com.peakda.server.domain.auth.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Apple 네이티브 로그인 요청")
data class AppleLoginRequest(
    @field:Schema(
        description = "iOS Apple 로그인 SDK 가 발급한 identity token (JWT)",
        example = "eyJraWQiOiJ...",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank
    val identityToken: String,
)
