package com.peakda.server.domain.notification.entity

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
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "device_tokens",
    uniqueConstraints = [UniqueConstraint(name = "uk_device_tokens_token", columnNames = ["token"])],
    indexes = [Index(name = "ix_device_tokens_user_id", columnList = "user_id")],
)
class DeviceToken(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, columnDefinition = "TEXT")
    val platform: DevicePlatform,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
