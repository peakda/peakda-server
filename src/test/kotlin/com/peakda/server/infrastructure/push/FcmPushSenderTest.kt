package com.peakda.server.infrastructure.push

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.SendResponse
import com.peakda.server.domain.notification.application.DeviceTokenService
import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.entity.DeviceToken
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class FcmPushSenderTest {

    private val messaging = mock(FirebaseMessaging::class.java)
    private val deviceTokenService = mock(DeviceTokenService::class.java)
    private val sender = FcmPushSender(messaging, deviceTokenService)

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

    @Test
    fun `무효 응답을 받은 토큰만 골라 도메인 서비스에 삭제를 위임한다`() {
        val response = batchResponse(
            failure(MessagingErrorCode.UNREGISTERED),
            success(),
            failure(MessagingErrorCode.INVALID_ARGUMENT),
        )
        `when`(messaging.sendEachForMulticast(anyMessage())).thenReturn(response)

        sender.send(deviceTokens("token-1", "token-2", "token-3"), PAYLOAD)

        verify(deviceTokenService).deleteInvalid(listOf("token-1", "token-3"))
    }

    @Test
    fun `일시 장애로 실패한 토큰은 삭제하지 않는다`() {
        val response = batchResponse(failure(MessagingErrorCode.UNAVAILABLE))
        `when`(messaging.sendEachForMulticast(anyMessage())).thenReturn(response)

        sender.send(deviceTokens("token-1"), PAYLOAD)

        verify(deviceTokenService).deleteInvalid(emptyList())
    }

    @Test
    fun `멀티캐스트 호출 자체가 실패하면 토큰을 삭제하지 않는다`() {
        val exception = mock(FirebaseMessagingException::class.java)
        `when`(messaging.sendEachForMulticast(anyMessage())).thenThrow(exception)

        sender.send(deviceTokens("token-1"), PAYLOAD)

        verify(deviceTokenService).deleteInvalid(emptyList())
    }

    private fun deviceTokens(vararg tokens: String): List<DeviceToken> =
        tokens.map { DeviceToken(userId = 1L, token = it, platform = DevicePlatform.ANDROID) }

    /** 스터빙 인자 안에서 만들면 중첩 스터빙이 되므로, 호출부에서 미리 만들어 넘긴다. */
    private fun batchResponse(vararg responses: SendResponse): BatchResponse =
        mock(BatchResponse::class.java).also { `when`(it.responses).thenReturn(responses.toList()) }

    private fun success(): SendResponse = mock(SendResponse::class.java)

    private fun failure(errorCode: MessagingErrorCode): SendResponse {
        val exception = mock(FirebaseMessagingException::class.java)
        `when`(exception.messagingErrorCode).thenReturn(errorCode)
        return mock(SendResponse::class.java).also { `when`(it.exception).thenReturn(exception) }
    }

    /** Mockito.any() 의 null 반환을 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun anyMessage(): MulticastMessage =
        any(MulticastMessage::class.java) ?: MulticastMessage.builder().addToken("dummy").build()

    companion object {
        private val PAYLOAD = PushPayload(
            title = "새 팔로워",
            body = "본문",
            linkType = NotificationLinkType.INTERNAL,
            linkUrl = null,
            targetId = 42L,
            notificationId = 9012L,
            type = NotificationType.FOLLOW,
        )
    }
}
