package com.peakda.server.domain.notification.application

import com.peakda.server.domain.feed.application.ReactionAddedEvent
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.user.application.FollowCreatedEvent
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.infrastructure.push.PushPayload
import com.peakda.server.infrastructure.push.PushSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 소셜 활동(팔로우·리액션) 이벤트를 원본 트랜잭션 커밋 후 수신해 알림을 생성한다.
 * 알림 저장은 [NotificationService] 자체 트랜잭션으로 먼저 커밋되고, 푸시 발송은 그 뒤 트랜잭션 밖에서 실행된다 —
 * 커밋되지 않은 알림이 발송되지 않고, 푸시 실패가 알림 저장을 롤백하지 않으며, 외부 호출 동안 DB 커넥션을 점유하지 않는다.
 */
@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val pushSender: PushSender,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onFollowCreated(event: FollowCreatedEvent) {
        val actor = userRepository.findById(event.followerId).orElse(null) ?: return
        createAndPush(
            CreateNotificationCommand(
                recipientId = event.followingId,
                actorUserId = event.followerId,
                type = NotificationType.FOLLOW,
                title = "새 팔로워",
                body = "${actor.nickname}님이 회원님을 팔로우했습니다.",
                linkType = NotificationLinkType.INTERNAL,
                targetId = event.followerId,
            ),
        )
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onReactionAdded(event: ReactionAddedEvent) {
        if (event.actorId == event.recordOwnerId) return
        val actor = userRepository.findById(event.actorId).orElse(null) ?: return
        createAndPush(
            CreateNotificationCommand(
                recipientId = event.recordOwnerId,
                actorUserId = event.actorId,
                type = NotificationType.REACTION,
                title = "새 반응",
                body = "${actor.nickname}님이 회원님의 기록에 반응했습니다.",
                linkType = NotificationLinkType.INTERNAL,
                targetId = event.recordId,
            ),
        )
    }

    private fun createAndPush(command: CreateNotificationCommand) {
        notificationService.create(command)
        val tokens = deviceTokenRepository.findByUserId(command.recipientId)
        if (tokens.isEmpty()) return

        pushSender.send(
            tokens,
            PushPayload(
                title = command.title,
                body = command.body,
                linkType = command.linkType,
                linkUrl = command.linkUrl,
                targetId = command.targetId,
            ),
        )
    }
}
