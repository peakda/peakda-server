package com.peakda.server.domain.auth.oauth.model

class GoogleOAuth2UserInfo(
    attributes: Map<String, Any>
) : OAuth2UserInfo(attributes) {

    override fun getProviderId(): String {
        return attributes["sub"]?.toString()
            ?: error("Google OAuth2 응답에 sub가 없습니다.")
    }

    override fun getEmail(): String? {
        return attributes["email"] as? String
    }

    override fun getProfileImageUrl(): String? {
        return attributes["picture"] as? String
    }
}
