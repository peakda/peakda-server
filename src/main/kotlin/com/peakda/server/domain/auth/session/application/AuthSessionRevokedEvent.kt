package com.peakda.server.domain.auth.session.application

/**
 * 다른 기기에서의 해제·기기 수 초과·로그아웃·탈취 의심으로 세션이 끝났을 때 발행한다.
 * 해당 기기의 푸시 토큰을 지워, 로그아웃된 기기로 그 사용자의 알림이 가지 않게 한다.
 * 같은 기기의 재로그인으로 세션이 교체될 때는 발행하지 않는다.
 */
data class AuthSessionRevokedEvent(
    val userId: Long,
    val deviceId: String,
)
