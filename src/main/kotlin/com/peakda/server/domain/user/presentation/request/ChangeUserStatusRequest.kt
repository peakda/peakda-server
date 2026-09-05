package com.peakda.server.domain.user.presentation.request

import com.peakda.server.domain.user.application.ChangeUserStatusCommand
import com.peakda.server.domain.user.entity.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "관리자 사용자 상태 변경 요청")
data class ChangeUserStatusRequest(
    @field:Schema(description = "변경할 상태. ACTIVE 또는 SUSPENDED", example = "SUSPENDED")
    val status: UserStatus,

    @field:Size(max = 500)
    @field:Schema(description = "제재 또는 해제 사유", example = "반복적인 운영 정책 위반", nullable = true)
    val memo: String? = null,
) {
    fun toCommand(): ChangeUserStatusCommand = ChangeUserStatusCommand(status = status, memo = memo)
}
