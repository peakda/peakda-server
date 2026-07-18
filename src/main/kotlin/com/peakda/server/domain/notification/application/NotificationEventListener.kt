package com.peakda.server.domain.notification.application

import com.peakda.server.domain.feed.application.ReactionAddedEvent
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.user.application.FollowCreatedEvent
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 소셜 활동(팔로우·리액션) 이벤트를 원본 트랜잭션 커밋 후 수신해 알림을 생성한다.
 * AFTER_COMMIT 이후에는 원본 트랜잭션이 종료돼 있으므로 REQUIRES_NEW 로 별도 트랜잭션에서 저장한다.
 */
@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onFollowCreated(event: FollowCreatedEvent) {
        val actor = userRepository.findById(event.followerId).orElse(null) ?: return
        notificationService.create(
            CreateNotificationCommand(
                recipientId = event.followingId,
                type = NotificationType.FOLLOW,
                title = "새 팔로워",
                body = "${actor.nickname}님이 회원님을 팔로우했습니다.",
                linkType = NotificationLinkType.INTERNAL,
                targetId = event.followerId,
            ),
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onReactionAdded(event: ReactionAddedEvent) {
        if (event.actorId == event.recordOwnerId) return
        val actor = userRepository.findById(event.actorId).orElse(null) ?: return
        notificationService.create(
            CreateNotificationCommand(
                recipientId = event.recordOwnerId,
                type = NotificationType.REACTION,
                title = "새 반응",
                body = "${actor.nickname}님이 회원님의 기록에 반응했습니다.",
                linkType = NotificationLinkType.INTERNAL,
                targetId = event.recordId,
            ),
        )
    }
}
