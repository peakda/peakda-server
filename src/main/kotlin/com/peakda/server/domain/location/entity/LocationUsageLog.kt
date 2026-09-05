package com.peakda.server.domain.location.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * 위치정보 이용·제공사실 확인자료 1건.
 *
 * 좌표 자체는 남기지 않는다. 확인자료에 필요한 것은 "누가·어떤 경로로·어떤 서비스에서·언제"
 * 개인위치정보를 이용했는지이며, 좌표를 함께 보관하면 불필요한 개인정보가 축적된다.
 */
@Entity
@Table(
    name = "location_usage_logs",
    indexes = [
        Index(name = "ix_location_usage_logs_user", columnList = "user_id,used_at"),
        Index(name = "ix_location_usage_logs_used_at", columnList = "used_at"),
    ],
)
class LocationUsageLog(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, columnDefinition = "TEXT")
    val channel: LocationAccessChannel,

    @Enumerated(EnumType.STRING)
    @Column(name = "service", nullable = false, columnDefinition = "TEXT")
    val service: LocationServiceType,

    @Column(name = "used_at", nullable = false)
    val usedAt: Instant,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
