package com.peakda.server.domain.spot.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "spot_favorites",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_spot_favorites_user_spot", columnNames = ["user_id", "spot_id"]),
    ],
    indexes = [
        Index(name = "ix_spot_favorites_user_id", columnList = "user_id"),
        Index(name = "ix_spot_favorites_spot_id", columnList = "spot_id"),
    ],
)
class SpotFavorite(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "spot_id", nullable = false)
    val spotId: Long,

    @Column(name = "notify_enabled", nullable = false)
    var notifyEnabled: Boolean = true,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
