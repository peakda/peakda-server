package com.peakda.server.domain.auth.oauth.model

abstract class OAuth2UserInfo(
    protected val attributes: Map<String, Any>
) {
    abstract fun getProviderId(): String
    abstract fun getEmail(): String?
    abstract fun getProfileImageUrl(): String?
}
