package com.peakda.server.domain.auth.session.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import org.springframework.data.domain.Persistable
import java.time.Instant
import java.util.UUID

/**
 * 로그인 세션(기기) 하나. 토큰의 sid 가 이 행을 가리킨다.
 * id 는 토큰을 만들기 전에 정해야 하므로 직접 할당하고, [Persistable] 로 신규 저장을 select 없이 insert 한다.
 */
@Entity
@Table(
    name = "auth_sessions",
    uniqueConstraints = [UniqueConstraint(name = "uk_auth_sessions_user_device", columnNames = ["user_id", "device_id"])],
    indexes = [Index(name = "ix_auth_sessions_user_id_last_used_at", columnList = "user_id, last_used_at")],
)
class AuthSession(
    @Id
    @Column(name = "id")
    private val id: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "device_id", nullable = false, columnDefinition = "TEXT")
    val deviceId: String,

    @Column(name = "device_name", columnDefinition = "TEXT")
    val deviceName: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, columnDefinition = "TEXT")
    val clientType: SessionClientType,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", columnDefinition = "TEXT")
    val platform: SessionPlatform?,

    @Column(name = "refresh_token_hash", nullable = false, columnDefinition = "TEXT")
    var refreshTokenHash: String,

    @Column(name = "last_used_at", nullable = false)
    var lastUsedAt: Instant,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
) : BaseTimeEntity(), Persistable<UUID> {

    @Column(name = "previous_refresh_token_hash", columnDefinition = "TEXT")
    var previousRefreshTokenHash: String? = null
        protected set

    @Column(name = "rotated_at")
    var rotatedAt: Instant? = null
        protected set

    @Transient
    private var newEntity: Boolean = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = newEntity

    @PostLoad
    @PostPersist
    fun markNotNew() {
        newEntity = false
    }

    fun isExpired(now: Instant): Boolean = !expiresAt.isAfter(now)

    fun isCurrent(refreshTokenHash: String): Boolean = this.refreshTokenHash == refreshTokenHash

    /**
     * 직전 토큰. 현재 토큰이 한 번이라도 쓰였다면 그때 회전되어 직전 토큰이 바뀌었을 것이므로,
     * 직전 토큰이 다시 왔다는 건 클라이언트가 현재 토큰을 받지 못했다는 뜻이다(동시 재발급·응답 유실).
     */
    fun isPrevious(refreshTokenHash: String): Boolean = previousRefreshTokenHash == refreshTokenHash

    /**
     * [presentedRefreshTokenHash] 로 재발급해 [newRefreshTokenHash] 를 현재 토큰으로 둔다.
     * 직전 토큰으로 다시 회전할 때도 직전 토큰은 그대로 두어, 받지 못한 토큰만 무효가 된다.
     */
    fun rotate(presentedRefreshTokenHash: String, newRefreshTokenHash: String, now: Instant, expiresAt: Instant) {
        previousRefreshTokenHash = presentedRefreshTokenHash
        refreshTokenHash = newRefreshTokenHash
        rotatedAt = now
        lastUsedAt = now
        this.expiresAt = expiresAt
    }
}
