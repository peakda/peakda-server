package com.peakda.server.common.security.principal

import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.entity.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User
import java.util.UUID

/**
 * @property sessionId 요청에 쓰인 액세스 토큰의 로그인 세션(기기). OAuth 로그인 중이거나 세션 도입 전 토큰이면 null.
 */
class PrincipalDetails(
    private val user: User,
    private val attributes: Map<String, Any> = emptyMap(),
    val sessionId: UUID? = null,
) : OAuth2User, UserDetails {

    fun getUser(): User = user

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("$ROLE_PREFIX${user.role.name}"))

    override fun getName(): String = user.id?.toString() ?: ""

    override fun getUsername(): String = user.id?.toString() ?: ""

    override fun getPassword(): String = ""

    override fun isEnabled(): Boolean = user.status == UserStatus.ACTIVE

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = user.status != UserStatus.SUSPENDED

    override fun isCredentialsNonExpired(): Boolean = true

    companion object {
        const val ROLE_PREFIX = "ROLE_"
    }
}
