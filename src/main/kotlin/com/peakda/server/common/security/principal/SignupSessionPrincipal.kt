package com.peakda.server.common.security.principal

import com.peakda.server.domain.auth.signup.entity.SignupSession
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class SignupSessionPrincipal(
    private val signupSession: SignupSession,
) {
    fun getSignupSession(): SignupSession = signupSession

    val authorities: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority(ROLE))

    companion object {
        const val ROLE = "ROLE_SIGNUP"
    }
}
