package com.peakda.server.infrastructure.push

import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FcmPushSenderTest {

    @Test
    fun `FCM data는 문서 필드를 문자열로 만들고 null 필드는 생략한다`() {
        val data = PushPayload(
            title = "새 공지",
            body = "본문",
            linkType = NotificationLinkType.EXTERNAL,
            linkUrl = "https://peakda.example/notice",
            targetId = null,
            notificationId = 9012L,
            type = NotificationType.NOTICE,
        ).data()

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "notificationId" to "9012",
                "type" to "NOTICE",
                "linkType" to "EXTERNAL",
                "linkUrl" to "https://peakda.example/notice",
            ),
        )
        assertThat(data).doesNotContainKey("targetId")
    }
}
