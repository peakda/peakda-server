package com.peakda.server.domain.notification.repository

import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByRecipientIdOrderByCreatedAtDesc(recipientId: Long, pageable: Pageable): Page<Notification>

    fun findByRecipientIdAndTypeInOrderByCreatedAtDesc(
        recipientId: Long,
        types: Collection<NotificationType>,
        pageable: Pageable,
    ): Page<Notification>

    /** 소유권까지 함께 확인해 남의 알림 읽음 시도를 막는다. */
    fun findByIdAndRecipientId(id: Long, recipientId: Long): Notification?

    fun countByRecipientIdAndReadAtIsNull(recipientId: Long): Long

    fun deleteByRecipientId(recipientId: Long)

    /** 안 읽은 알림을 한 번에 읽음 처리한다. */
    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.recipientId = :recipientId AND n.readAt IS NULL")
    fun markAllRead(@Param("recipientId") recipientId: Long, @Param("now") now: Instant)
}
