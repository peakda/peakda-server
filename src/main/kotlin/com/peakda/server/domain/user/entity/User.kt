package com.peakda.server.domain.user.entity

import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = ["provider", "provider_id"]),
        UniqueConstraint(name = "uk_users_nickname", columnNames = ["nickname"]),
    ],
)
class User(
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, columnDefinition = "TEXT")
    val provider: OAuth2LoginType,

    @Column(name = "provider_id", nullable = false, columnDefinition = "TEXT")
    val providerId: String,

    @Column(name = "nickname", nullable = false, columnDefinition = "TEXT")
    var nickname: String,

    @Column(name = "email", columnDefinition = "TEXT")
    var email: String? = null,

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    var profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: UserStatus = UserStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, columnDefinition = "TEXT")
    var role: UserRole = UserRole.USER,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    companion object {
        fun create(
            provider: OAuth2LoginType,
            providerId: String,
            nickname: String,
            email: String?,
            profileImageUrl: String?,
        ): User = User(
            provider = provider,
            providerId = providerId,
            nickname = nickname,
            email = email,
            profileImageUrl = profileImageUrl,
            status = UserStatus.ACTIVE,
        )
    }
}
