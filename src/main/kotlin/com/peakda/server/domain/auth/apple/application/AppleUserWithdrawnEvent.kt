package com.peakda.server.domain.auth.apple.application

/**
 * 애플 회원이 탈퇴했다. 탈퇴 처리가 users.provider_id 를 익명화하므로 원래 애플 사용자 식별자를 함께 싣는다.
 */
data class AppleUserWithdrawnEvent(
    val userId: Long,
    val appleUserId: String,
)
