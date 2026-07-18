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
import java.time.Instant

/**
 * 사용자 알림(P3-1, SCR-012~012c). 4종(TIMING/FOLLOW/REACTION/NOTICE)을 단일 테이블에 적재하고
 * 세그먼트(탭)별 조회·읽음 처리를 지원한다. 공지 라우팅은 [linkType]/[linkUrl]/[targetId] 메타로 클라가 분기한다 (결정 E).
 */
@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "ix_notifications_recipient", columnList = "recipient_id,created_at"),
        Index(name = "ix_notifications_recipient_read", columnList = "recipient_id,read_at"),
    ],
)
class Notification(
    @Column(name = "recipient_id", nullable = false)
    val recipientId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "TEXT")
    val type: NotificationType,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    val title: String,

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    val body: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, columnDefinition = "TEXT")
    val linkType: NotificationLinkType,

    @Column(name = "link_url", columnDefinition = "TEXT")
    val linkUrl: String? = null,

    @Column(name = "target_id")
    val targetId: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    @Column(name = "read_at")
    var readAt: Instant? = null
        protected set

    /** 최초 1회만 읽음 시각을 기록한다 (이미 읽었으면 무시). */
    fun markRead(now: Instant) {
        if (readAt == null) readAt = now
    }
}
