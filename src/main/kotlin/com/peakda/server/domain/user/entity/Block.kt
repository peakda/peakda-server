package com.peakda.server.domain.user.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 사용자 간 차단 관계. `blocker` 가 `blocked` 를 차단한다 (SCR-024h, P2-4).
 */
@Entity
@Table(
    name = "user_blocks",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_blocks_blocker_blocked", columnNames = ["blocker_id", "blocked_id"]),
    ],
    indexes = [
        Index(name = "ix_user_blocks_blocker_id", columnList = "blocker_id"),
        Index(name = "ix_user_blocks_blocked_id", columnList = "blocked_id"),
    ],
)
class Block(
    @Column(name = "blocker_id", nullable = false)
    val blockerId: Long,

    @Column(name = "blocked_id", nullable = false)
    val blockedId: Long,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
