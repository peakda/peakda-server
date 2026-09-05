package com.peakda.server.domain.auth.signup.application

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.domain.auth.oauth.model.OAuth2UserInfo
import com.peakda.server.domain.auth.signup.entity.SignupSession
import com.peakda.server.domain.auth.signup.repository.SignupSessionRepository
import com.peakda.server.common.security.cookie.CookieProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SignupSessionService(
    private val signupSessionRepository: SignupSessionRepository,
    private val cookieProperties: CookieProperties,
) {

    @Transactional
    fun createOrRefresh(type: OAuth2LoginType, info: OAuth2UserInfo): SignupSession {
        val token = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plusSeconds(cookieProperties.signupTokenValidityInSeconds)
        val existing = signupSessionRepository.findByProviderAndProviderId(type, info.getProviderId())

        if (existing != null) {
            existing.refresh(
                token = token,
                email = info.getEmail(),
                profileImageUrl = info.getProfileImageUrl(),
                expiresAt = expiresAt,
            )
            return existing
        }

        return signupSessionRepository.save(
            SignupSession(
                token = token,
                provider = type,
                providerId = info.getProviderId(),
                email = info.getEmail(),
                profileImageUrl = info.getProfileImageUrl(),
                expiresAt = expiresAt,
            )
        )
    }

    @Transactional(readOnly = true)
    fun findValidByToken(token: String): SignupSession? {
        val session = signupSessionRepository.findByToken(token) ?: return null
        return session.takeUnless { it.isExpired() }
    }
}
