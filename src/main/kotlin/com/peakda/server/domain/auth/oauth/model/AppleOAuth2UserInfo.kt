package com.peakda.server.domain.auth.oauth.model

/**
 * Apple id_token 검증 결과로부터 만든 사용자 정보.
 *
 * 다른 provider 와 달리 OAuth2 userinfo 응답 맵이 아니라 검증된 id_token 클레임의
 * `sub`(providerId)·`email` 로 구성한다. Apple 은 이름·프로필 이미지를 토큰에 담지 않으므로
 * [getProfileImageUrl] 은 항상 null 이다.
 */
class AppleOAuth2UserInfo(
    private val sub: String,
    private val email: String?,
) : OAuth2UserInfo(emptyMap()) {

    override fun getProviderId(): String = sub

    override fun getEmail(): String? = email

    override fun getProfileImageUrl(): String? = null
}
