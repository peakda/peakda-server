package com.peakda.server.common.security.principal

import com.peakda.server.domain.auth.signup.entity.SignupSession
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

class OAuth2SignupPrincipal(
    private val signupSession: SignupSession,
    private val attributes: Map<String, Any> = emptyMap(),
) : OAuth2User {

    fun getSignupSession(): SignupSession = signupSession

    override fun getName(): String = signupSession.id?.toString() ?: signupSession.providerId

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority(SignupSessionPrincipal.ROLE))
}
