package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.Notice
import com.peakda.server.domain.notification.entity.NoticeStatus
import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.exception.NoticeNotFoundException
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.notification.repository.NoticeDispatchRepository
import com.peakda.server.domain.notification.repository.NoticeRepository
import com.peakda.server.domain.notification.repository.NotificationRepository
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.infrastructure.push.PushPayload
import com.peakda.server.infrastructure.push.PushSender
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

@Service
class NoticeFanoutService(
    private val noticeRepository: NoticeRepository,
    private val noticeDispatchRepository: NoticeDispatchRepository,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val pushSender: PushSender,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun dispatchPending(): Int {
        val notice = noticeRepository.findFirstByStatusOrderByIdAsc(NoticeStatus.DISPATCHING) ?: return 0
        val noticeId = requireNotNull(notice.id)
        var cursor = 0L
        var dispatched = 0

        while (true) {
            val users = userRepository.findTop500ByStatusAndIdGreaterThanOrderByIdAsc(
                status = UserStatus.ACTIVE,
                id = cursor,
            )
            if (users.isEmpty()) break

            val userIds = users.map { requireNotNull(it.id) }
            val insertedUserIds = transactionTemplate.execute {
                recordNotifications(notice, noticeId, userIds)
            }.orEmpty()

            push(notice, insertedUserIds)
            dispatched += insertedUserIds.size
            cursor = userIds.last()
            if (users.size < PAGE_SIZE) break
        }

        transactionTemplate.executeWithoutResult {
            val managedNotice = noticeRepository.findByIdForUpdate(noticeId) ?: throw NoticeNotFoundException()
            if (managedNotice.status == NoticeStatus.DISPATCHING) {
                managedNotice.completeDispatch(
                    dispatchedAt = Instant.now(),
                    sentCount = noticeDispatchRepository.countByNoticeId(noticeId).toInt(),
                )
            }
        }
        return dispatched
    }

    private fun recordNotifications(notice: Notice, noticeId: Long, userIds: List<Long>): List<Long> {
        val insertedUserIds = noticeDispatchRepository.insertIfAbsent(noticeId, userIds.toLongArray())
        if (insertedUserIds.isEmpty()) return emptyList()

        notificationRepository.saveAll(
            insertedUserIds.map { userId ->
                Notification(
                    recipientId = userId,
                    type = NotificationType.NOTICE,
                    title = notice.title,
                    body = notice.body,
                    linkType = notice.linkType,
                    linkUrl = notice.linkUrl,
                    targetId = notice.targetId,
                )
            },
        )
        return insertedUserIds
    }

    private fun push(notice: Notice, userIds: List<Long>) {
        if (userIds.isEmpty()) return
        val tokens = deviceTokenRepository.findByUserIdIn(userIds)
        if (tokens.isEmpty()) return

        pushSender.send(
            tokens,
            PushPayload(
                title = notice.title,
                body = notice.body,
                linkType = notice.linkType,
                linkUrl = notice.linkUrl,
                targetId = notice.targetId,
            ),
        )
    }

    companion object {
        private const val PAGE_SIZE = 500
    }
}
