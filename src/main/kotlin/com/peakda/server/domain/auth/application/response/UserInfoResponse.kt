package com.peakda.server.domain.auth.application.response

import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus

data class UserInfoResponse(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val profileImageUrl: String?,
    val status: UserStatus
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
