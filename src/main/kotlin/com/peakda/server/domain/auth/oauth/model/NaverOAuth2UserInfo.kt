package com.peakda.server.domain.auth.oauth.model

class NaverOAuth2UserInfo(
    attributes: Map<String, Any>
) : OAuth2UserInfo(attributes) {

    @Suppress("UNCHECKED_CAST")
    private val response: Map<String, Any> =
        attributes["response"] as? Map<String, Any> ?: emptyMap()

    override fun getProviderId(): String {
        return response["id"]?.toString()
            ?: error("Naver OAuth2 응답에 id가 없습니다.")
    }

    override fun getEmail(): String? {
        return response["email"] as? String
    }

    override fun getProfileImageUrl(): String? {
        return response["profile_image"] as? String
    }
}
