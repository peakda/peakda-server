package com.peakda.server.infrastructure.push

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.peakda.server.domain.notification.application.DeviceTokenService
import com.peakda.server.domain.notification.entity.DeviceToken
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/** Firebase Admin SDK FCM adapter. It is created only when FCM delivery is enabled. */
@Component
@ConditionalOnProperty(prefix = "app.push", name = ["enabled"], havingValue = "true")
class FcmPushSender(
    private val messaging: FirebaseMessaging,
    private val deviceTokenService: DeviceTokenService,
) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(tokens: List<DeviceToken>, payload: PushPayload) {
        if (tokens.isEmpty()) return

        tokens.map { it.token }.distinct().chunked(MAX_MULTICAST_SIZE).forEach { chunk ->
            deviceTokenService.deleteInvalid(dispatch(chunk, payload))
        }
    }

    /** 한 묶음을 발송하고 무효 응답을 받은 토큰을 돌려준다. */
    private fun dispatch(tokens: List<String>, payload: PushPayload): List<String> {
        val message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(Notification.builder().setTitle(payload.title).setBody(payload.body).build())
            .putAllData(payload.data())
            .build()
        val response = try {
            messaging.sendEachForMulticast(message)
        } catch (exception: FirebaseMessagingException) {
            log.warn("FCM multicast failed; retaining tokens for retry: tokenCount={}", tokens.size, exception)
            return emptyList()
        }
        return invalidTokens(tokens, response)
    }

    private fun invalidTokens(tokens: List<String>, response: BatchResponse): List<String> =
        response.responses.mapIndexedNotNull { index, result ->
            val errorCode = result.exception?.messagingErrorCode ?: return@mapIndexedNotNull null
            tokens[index].takeIf { errorCode in INVALID_TOKEN_ERRORS }
        }

    companion object {
        private const val MAX_MULTICAST_SIZE = 500
        private val INVALID_TOKEN_ERRORS = setOf(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
        )
    }
}

internal fun PushPayload.data(): Map<String, String> = buildMap {
    put("notificationId", notificationId.toString())
    put("type", type.name)
    put("linkType", linkType.name)
    targetId?.let { put("targetId", it.toString()) }
    linkUrl?.let { put("linkUrl", it) }
}
