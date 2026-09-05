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

    @field:Schema(description = "EXTERNAL 일 때 이동할 외부 링크. INTERNAL 일 때는 사용하지 않는다. 프론트가 type + targetId 로 경로를 조합한다.", example = "https://peakda.notion.site/notice")
    val linkUrl: String?,

    @field:Schema(description = "타입별 이동 대상 id. TIMING은 spotId, FOLLOW는 팔로우한 사람의 userId, REACTION은 recordId, NOTICE는 관리자가 지정한 값이다.", example = "42")
    val targetId: Long?,

    @field:Schema(description = "FOLLOW/REACTION 알림 행위자의 프로필 이미지 URL. TIMING/NOTICE는 null이다.", example = "https://cdn.example.com/profile/42.jpg")
    val imageUrl: String?,

    @field:Schema(description = "읽음 여부", example = "false")
    val read: Boolean,

    @field:Schema(description = "생성 시각", example = "2026-07-04T09:41:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification, actorImageUrl: String? = null): NotificationResponse = NotificationResponse(
            id = requireNotNull(notification.id),
            type = notification.type,
            title = notification.title,
            body = notification.body,
            linkType = notification.linkType,
            linkUrl = notification.linkUrl,
            targetId = notification.targetId,
            imageUrl = when (notification.type) {
                NotificationType.FOLLOW, NotificationType.REACTION -> actorImageUrl
                NotificationType.TIMING, NotificationType.NOTICE -> null
            },
            read = notification.readAt != null,
            createdAt = notification.createdAt,
        )
    }
}
