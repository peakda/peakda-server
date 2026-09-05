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
 * 사용자 간 팔로우 관계. `follower` 가 `following` 을 팔로우한다.
 *  - followerId: 팔로우를 하는 주체
 *  - followingId: 팔로우 대상
 *
 * 즉 "A 의 팔로워 목록" = followingId = A 인 행들, "A 의 팔로잉 목록" = followerId = A 인 행들.
 */
@Entity
@Table(
    name = "follows",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_follows_follower_following", columnNames = ["follower_id", "following_id"]),
    ],
    indexes = [
        Index(name = "ix_follows_follower_id", columnList = "follower_id"),
        Index(name = "ix_follows_following_id", columnList = "following_id"),
    ],
)
class Follow(
    @Column(name = "follower_id", nullable = false)
    val followerId: Long,

    @Column(name = "following_id", nullable = false)
    val followingId: Long,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
