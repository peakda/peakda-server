package com.peakda.server.domain.auth.app.presentation.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "일회성 코드 교환 요청")
data class AppTokenExchangeRequest(
    @field:NotBlank
    @field:Schema(description = "딥링크로 받은 일회성 코드", example = "Zm9vYmFyYmF6cXV4")
    val code: String,
)
