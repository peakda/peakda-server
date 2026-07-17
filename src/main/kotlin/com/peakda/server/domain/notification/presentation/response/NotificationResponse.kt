package com.peakda.server.domain.notification.presentation.response

import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "알림 1건 (SCR-012~012c)")
data class NotificationResponse(
    @field:Schema(description = "알림 PK", example = "9012")
    val id: Long,

    @field:Schema(description = "알림 종류", example = "FOLLOW")
    val type: NotificationType,

    @field:Schema(description = "제목", example = "새 팔로워")
    val title: String,

    @field:Schema(description = "본문", example = "벚꽃러버님이 회원님을 팔로우했습니다.")
    val body: String,

    @field:Schema(description = "탭 시 이동 방식", example = "INTERNAL")
    val linkType: NotificationLinkType,

    @field:Schema(description = "EXTERNAL 일 때 이동할 외부 링크", example = "https://peakda.notion.site/notice")
    val linkUrl: String?,

    @field:Schema(description = "INTERNAL 일 때 이동 대상 id (팔로워 id·기록 id·스팟 id 등)", example = "42")
    val targetId: Long?,

    @field:Schema(description = "읽음 여부", example = "false")
    val read: Boolean,

    @field:Schema(description = "생성 시각", example = "2026-07-04T09:41:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification): NotificationResponse = NotificationResponse(
            id = requireNotNull(notification.id),
            type = notification.type,
            title = notification.title,
            body = notification.body,
            linkType = notification.linkType,
            linkUrl = notification.linkUrl,
            targetId = notification.targetId,
            read = notification.readAt != null,
            createdAt = notification.createdAt,
        )
    }
}
