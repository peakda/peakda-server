package com.peakda.server.domain.auth.application.response

import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "현재 로그인한 사용자 정보")
data class UserInfoResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "이메일", example = "user@example.com", nullable = true)
    val email: String?,
    @field:Schema(description = "닉네임", example = "peakda", nullable = true)
    val nickname: String?,
    @field:Schema(
        description = "프로필 이미지 URL",
        example = "https://k.kakaocdn.net/dn/profile.jpg",
        nullable = true,
    )
    val profileImageUrl: String?,
    @field:Schema(description = "사용자 상태", example = "ACTIVE")
    val status: UserStatus,
) {
    companion object {
        fun from(user: User): UserInfoResponse {
            return UserInfoResponse(
                id = requireNotNull(user.id),
                email = user.email,
                nickname = user.nickname,
                profileImageUrl = user.profileImageUrl,
                status = user.status
            )
        }
    }
}
