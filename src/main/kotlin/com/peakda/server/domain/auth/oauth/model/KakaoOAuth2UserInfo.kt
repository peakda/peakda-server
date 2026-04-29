package com.peakda.server.domain.auth.oauth.model

class KakaoOAuth2UserInfo(
    attributes: Map<String, Any>
) : OAuth2UserInfo(attributes) {

    @Suppress("UNCHECKED_CAST")
    private val kakaoAccount: Map<String, Any> =
        attributes["kakao_account"] as? Map<String, Any> ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    private val profile: Map<String, Any> =
        kakaoAccount["profile"] as? Map<String, Any> ?: emptyMap()

    override fun getProviderId(): String {
        return attributes["id"]?.toString()
            ?: error("Kakao OAuth2 응답에 id가 없습니다.")
    }

    override fun getEmail(): String? {
        return kakaoAccount["email"] as? String
    }

    override fun getProfileImageUrl(): String? {
        return profile["profile_image_url"] as? String
    }
}
