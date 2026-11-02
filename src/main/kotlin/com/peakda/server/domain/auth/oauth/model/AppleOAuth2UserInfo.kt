package com.peakda.server.domain.auth.oauth.model

/**
 * 검증을 마친 애플 identity token 의 클레임.
 * 애플은 사용자 정보 API 가 없고 프로필 사진도 주지 않는다. 이메일은 사용자가 가리기를 고르면
 * `@privaterelay.appleid.com` 릴레이 주소다.
 */
class AppleOAuth2UserInfo(
    claims: Map<String, Any>,
) : OAuth2UserInfo(claims) {

    override fun getProviderId(): String {
        return attributes["sub"]?.toString()
            ?: error("애플 identity token 에 sub 가 없습니다.")
    }

    override fun getEmail(): String? {
        return attributes["email"] as? String
    }

    override fun getProfileImageUrl(): String? = null
}
