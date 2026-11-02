package com.peakda.server.domain.auth.oauth.model

enum class OAuth2LoginType(val provider: String) {
    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google"),

    // iOS 앱 전용. 앱이 받은 identity token 을 /api/auth/app/apple 로 검증한다.
    // Spring OAuth2 클라이언트 등록이 없어 웹 로그인(/oauth2/authorization/*) 경로로는 들어오지 않는다.
    APPLE("apple");

    companion object {
        fun from(provider: String): OAuth2LoginType? =
            entries.firstOrNull { it.provider.equals(provider, ignoreCase = true) }
    }
}
