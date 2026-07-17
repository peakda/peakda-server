package com.peakda.server.domain.notification.entity

enum class NotificationType {
    /** 찜한 스팟의 만개 임박 알림 (P3-3) */
    TIMING,

    /** 다른 사용자가 나를 팔로우함 */
    FOLLOW,

    /** 내 기록에 리액션이 달림 */
    REACTION,

    /** 운영 공지 */
    NOTICE,
}
