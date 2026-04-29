package com.peakda.server.domain.user.repository

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderId(provider: OAuth2LoginType, providerId: String): User?
    fun existsByNickname(nickname: String): Boolean
}
