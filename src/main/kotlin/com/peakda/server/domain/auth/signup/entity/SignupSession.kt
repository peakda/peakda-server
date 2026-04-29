package com.peakda.server.domain.auth.signup.entity

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.global.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "signup_sessions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_signup_sessions_token", columnNames = ["token"]),
        UniqueConstraint(name = "uk_signup_sessions_provider_provider_id", columnNames = ["provider", "provider_id"]),
    ],
)
class SignupSession(
    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    var token: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, columnDefinition = "TEXT")
    val provider: OAuth2LoginType,

    @Column(name = "provider_id", nullable = false, columnDefinition = "TEXT")
    val providerId: String,

    @Column(name = "email", columnDefinition = "TEXT")
    var email: String? = null,

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    var profileImageUrl: String? = null,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    fun refresh(token: String, email: String?, profileImageUrl: String?, expiresAt: Instant) {
        this.token = token
        this.email = email
        this.profileImageUrl = profileImageUrl
        this.expiresAt = expiresAt
    }

    fun isExpired(now: Instant = Instant.now()): Boolean = !expiresAt.isAfter(now)
}
