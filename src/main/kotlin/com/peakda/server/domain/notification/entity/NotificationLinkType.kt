package com.peakda.server.domain.notification.entity

/**
 * 알림 탭 시 이동 방식 (결정 E). V1 운영은 EXTERNAL(노션/웹뷰) 우선이며, 내부 상세는 후속에서 INTERNAL 로 추가한다.
 */
enum class NotificationLinkType {
    /** 앱 내부 화면 이동 (targetId 사용) */
    INTERNAL,

    /** 외부 링크 이동 (linkUrl 사용) */
    EXTERNAL,
}
