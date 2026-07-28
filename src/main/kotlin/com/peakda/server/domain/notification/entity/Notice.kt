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

@Entity
@Table(
    name = "notices",
    indexes = [Index(name = "ix_notices_status_id", columnList = "status,id")],
)
class Notice(
    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    var body: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, columnDefinition = "TEXT")
    var linkType: NotificationLinkType,

    @Column(name = "link_url", columnDefinition = "TEXT")
    var linkUrl: String? = null,

    @Column(name = "target_id")
    var targetId: Long? = null,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: NoticeStatus = NoticeStatus.DRAFT
        protected set

    @Column(name = "dispatched_at")
    var dispatchedAt: Instant? = null
        protected set

    @Column(name = "sent_count", nullable = false)
    var sentCount: Int = 0
        protected set

    fun update(
        title: String,
        body: String,
        linkType: NotificationLinkType,
        linkUrl: String?,
        targetId: Long?,
    ) {
        this.title = title
        this.body = body
        this.linkType = linkType
        this.linkUrl = linkUrl
        this.targetId = targetId
    }

    fun startDispatch() {
        status = NoticeStatus.DISPATCHING
    }

    fun cancel() {
        status = NoticeStatus.CANCELED
    }

    fun completeDispatch(dispatchedAt: Instant, sentCount: Int) {
        status = NoticeStatus.DISPATCHED
        this.dispatchedAt = dispatchedAt
        this.sentCount = sentCount
    }
}
