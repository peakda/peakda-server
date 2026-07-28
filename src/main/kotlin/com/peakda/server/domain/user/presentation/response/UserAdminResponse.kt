package com.peakda.server.domain.user.presentation.response

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "관리자 사용자 조회 응답")
data class UserAdminResponse(
    @field:Schema(description = "사용자 id", example = "31")
    val id: Long,
    @field:Schema(description = "닉네임", example = "꽃길여행자")
    val nickname: String,
    @field:Schema(description = "이메일", example = "traveler@example.com", nullable = true)
    val email: String?,
    @field:Schema(description = "OAuth2 제공자", example = "KAKAO")
    val provider: OAuth2LoginType,
    @field:Schema(description = "사용자 상태", example = "ACTIVE")
    val status: UserStatus,
    @field:Schema(description = "사용자 역할", example = "USER")
    val role: UserRole,
    @field:Schema(description = "가입 시각", example = "2026-07-28T09:30:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(user: User): UserAdminResponse = UserAdminResponse(
            id = requireNotNull(user.id),
            nickname = user.nickname,
            email = user.email,
            provider = user.provider,
            status = user.status,
            role = user.role,
            createdAt = user.createdAt,
        )
    }
}
