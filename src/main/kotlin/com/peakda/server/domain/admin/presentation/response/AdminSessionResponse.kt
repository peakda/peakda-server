package com.peakda.server.domain.admin.presentation.response

import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "현재 관리자 세션")
data class AdminSessionResponse(
    @field:Schema(description = "관리자 사용자 id", example = "7")
    val userId: Long,

    @field:Schema(description = "관리자 닉네임", example = "운영자")
    val nickname: String,

    @field:Schema(description = "사용자 역할", example = "ADMIN")
    val role: UserRole,
) {
    companion object {
        fun from(user: User): AdminSessionResponse = AdminSessionResponse(
            userId = requireNotNull(user.id),
            nickname = user.nickname,
            role = user.role,
        )
    }
}
