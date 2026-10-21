package com.peakda.server.domain.launchalert.entity

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
import java.time.Instant

/**
 * 랜딩 페이지의 앱 출시 알림 신청 1건.
 *
 * 회원과 무관한 개인정보라 users 와 연결하지 않는다. 출시 안내를 보내면 목적을 다하므로 관리자가 파기한다.
 * 저장은 같은 이메일 재신청을 한 번에 처리하려고 레포지토리의 upsert 로만 한다.
 * 파기는 [exportedAt] 이 찍힌 신청, 즉 관리자가 실제로 내려받아 메일을 보낸 신청만 대상으로 한다.
 */
@Entity
@Table(
    name = "launch_alert_subscriptions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_launch_alert_subscriptions_platform_email", columnNames = ["platform", "email"]),
    ],
)
class LaunchAlertSubscription(
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, columnDefinition = "TEXT")
    val platform: LaunchAlertPlatform,

    @Column(name = "email", nullable = false, columnDefinition = "TEXT")
    val email: String,

    @Column(name = "consent_version", nullable = false, columnDefinition = "TEXT")
    val consentVersion: String,

    @Column(name = "consented_at", nullable = false)
    val consentedAt: Instant,
) : BaseTimeEntity() {

    @Column(name = "exported_at")
    var exportedAt: Instant? = null
        protected set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
