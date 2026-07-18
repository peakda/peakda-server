package com.peakda.server.infrastructure.push

import com.peakda.server.domain.notification.entity.NotificationLinkType

data class PushPayload(
    val title: String,
    val body: String,
    val linkType: NotificationLinkType,
    val linkUrl: String?,
    val targetId: Long?,
)
