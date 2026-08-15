package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType

/**
 * 알림 1건 생성 입력. [NotificationService.create] 의 유일한 입력 객체다.
 */
data class CreateNotificationCommand(
    val recipientId: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val linkType: NotificationLinkType = NotificationLinkType.INTERNAL,
    val linkUrl: String? = null,
    val targetId: Long? = null,
    val actorUserId: Long? = null,
)
