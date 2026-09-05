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
    var providerId: String,

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

    /**
     * 계정 탈퇴 처리. 상태를 DEACTIVATED 로 전환하고 개인식별정보를 익명화한다.
     *
     * - 닉네임은 탈퇴 식별자로 치환해 기존 닉네임을 다른 사용자가 재사용할 수 있게 한다.
     * - providerId 를 무효화해 (provider, provider_id) 유니크 제약을 비워, 동일 소셜 계정의
     *   재가입을 복구가 아닌 신규 가입으로 처리한다 (결정 G).
     */
    fun withdraw() {
        val currentId = requireNotNull(id) { "탈퇴하려는 사용자 id 가 없습니다." }
        status = UserStatus.DEACTIVATED
        nickname = "$WITHDRAWN_PREFIX$currentId"
        email = null
        profileImageUrl = null
        providerId = "$WITHDRAWN_PREFIX$currentId:$providerId"
    }

    companion object {
        private const val WITHDRAWN_PREFIX = "withdrawn-"

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
