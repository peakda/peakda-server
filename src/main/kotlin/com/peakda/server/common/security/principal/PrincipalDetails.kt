package com.peakda.server.common.security.principal

import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User

class PrincipalDetails(
    private val user: User,
    private val attributes: Map<String, Any> = emptyMap(),
) : OAuth2User, UserDetails {

    fun getUser(): User = user

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("$ROLE_PREFIX${user.role.name}"))

    override fun getName(): String = user.id?.toString() ?: ""

    override fun getUsername(): String = user.id?.toString() ?: ""

    override fun getPassword(): String = ""

    override fun isEnabled(): Boolean = user.status != UserStatus.DEACTIVATED

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = user.status != UserStatus.DEACTIVATED

    override fun isCredentialsNonExpired(): Boolean = true

    companion object {
        const val ROLE_PREFIX = "ROLE_"
    }
}
