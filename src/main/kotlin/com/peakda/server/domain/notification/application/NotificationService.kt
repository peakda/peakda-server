package com.peakda.server.domain.notification.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationSegment
import com.peakda.server.domain.notification.exception.NotificationNotFoundException
import com.peakda.server.domain.notification.presentation.response.NotificationResponse
import com.peakda.server.domain.notification.repository.NotificationRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 알림 저장·조회·읽음(P3-1). 생성은 이벤트 리스너·스케줄러가 호출하고, 조회·읽음은 사용자 요청으로 호출된다.
 */
@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {

    fun create(command: CreateNotificationCommand): Notification {
        val notification = Notification(
            recipientId = command.recipientId,
            actorUserId = command.actorUserId,
            type = command.type,
            title = command.title,
            body = command.body,
            linkType = command.linkType,
            linkUrl = command.linkUrl,
            targetId = command.targetId,
        )
        return notificationRepository.save(notification)
    }

    @Transactional(readOnly = true)
    fun list(recipientId: Long, segment: NotificationSegment, pageRequest: PageRequest): PageResponse<NotificationResponse> {
        val pageable = pageRequest.toPageable()
        val page = segment.types
            ?.let { notificationRepository.findByRecipientIdAndTypeInOrderByCreatedAtDesc(recipientId, it, pageable) }
            ?: notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
        val actorIds = page.content.mapNotNull { it.actorUserId }.distinct()
        val usersById = if (actorIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(actorIds).associateBy { requireNotNull(it.id) }
        }
        return page.map { notification ->
            val imageUrl = notification.actorUserId
                ?.let { usersById[it]?.profileImageUrl }
                ?.let(objectKeyUrlResolver::resolve)
            NotificationResponse.from(notification, imageUrl)
        }.toPageResponse()
    }

    @Transactional(readOnly = true)
    fun unreadCount(recipientId: Long): Long = notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId)

    fun markRead(recipientId: Long, id: Long) {
        val notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
            ?: throw NotificationNotFoundException()
        notification.markRead(Instant.now())
    }

    fun markAllRead(recipientId: Long) {
        notificationRepository.markAllRead(recipientId, Instant.now())
    }

    /** 계정 탈퇴 시 사용. */
    fun deleteAllByUser(recipientId: Long) {
        notificationRepository.deleteByRecipientId(recipientId)
    }
}
