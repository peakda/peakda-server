package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.NotificationLinkType

data class UpsertNoticeCommand(
    val title: String,
    val body: String,
    val linkType: NotificationLinkType,
    val linkUrl: String?,
    val targetId: Long?,
)
