package com.peakda.server.domain.auth.oauth.model

enum class OAuth2LoginType(val provider: String) {
    KAKAO("kakao"),
    NAVER("naver"),
    APPLE("apple");

    companion object {
        fun from(provider: String): OAuth2LoginType? =
            entries.firstOrNull { it.provider.equals(provider, ignoreCase = true) }
    }
}
