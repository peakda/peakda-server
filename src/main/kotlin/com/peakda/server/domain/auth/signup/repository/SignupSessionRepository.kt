package com.peakda.server.domain.auth.signup.repository

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.signup.entity.SignupSession
import org.springframework.data.jpa.repository.JpaRepository

interface SignupSessionRepository : JpaRepository<SignupSession, Long> {
    fun findByToken(token: String): SignupSession?
    fun findByProviderAndProviderId(provider: OAuth2LoginType, providerId: String): SignupSession?
    fun deleteByToken(token: String)
}
