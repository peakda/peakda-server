package com.peakda.server.domain.notification.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "notice_dispatches",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notice_dispatches_notice_user",
            columnNames = ["notice_id", "user_id"],
        ),
    ],
)
class NoticeDispatch(
    @Column(name = "notice_id", nullable = false)
    val noticeId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: Instant
        protected set
}
