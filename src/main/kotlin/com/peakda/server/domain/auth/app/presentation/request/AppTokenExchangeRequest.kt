package com.peakda.server.domain.auth.app.presentation.request

import com.peakda.server.domain.auth.session.presentation.request.SessionDeviceRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

@Schema(description = "일회성 코드 교환 요청")
data class AppTokenExchangeRequest(
    @field:NotBlank
    @field:Schema(description = "딥링크로 받은 일회성 코드", example = "Zm9vYmFyYmF6cXV4")
    val code: String,

    @field:Valid
    @field:Schema(description = "로그인 기기 정보. 구버전 앱은 생략할 수 있다.", nullable = true)
    val device: SessionDeviceRequest? = null,
)
