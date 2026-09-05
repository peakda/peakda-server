package com.peakda.server.domain.notification.entity

/**
 * 알림함 세그먼트(탭) 필터 (SCR-012~012c). [types] 가 null 이면 전체 조회, 아니면 해당 타입만 조회한다.
 */
enum class NotificationSegment(val types: List<NotificationType>?) {
    ALL(null),
    TIMING(listOf(NotificationType.TIMING)),
    ACTIVITY(listOf(NotificationType.FOLLOW, NotificationType.REACTION)),
    NOTICE(listOf(NotificationType.NOTICE)),
}
