package com.peakda.server.infrastructure.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.peakda.server.domain.notification.entity.DeviceToken
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.Base64

/** Firebase Admin SDK FCM adapter. It is created only when FCM delivery is enabled. */
@Component
@ConditionalOnProperty(prefix = "app.push", name = ["enabled"], havingValue = "true")
class FcmPushSender(
    private val properties: FcmProperties,
    private val deviceTokenRepository: DeviceTokenRepository,
) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)
    private val messaging: FirebaseMessaging = initializeMessaging()

    override fun send(tokens: List<DeviceToken>, payload: PushPayload) {
        if (tokens.isEmpty()) return

        tokens.map { it.token }.distinct().chunked(MAX_MULTICAST_SIZE).forEach { chunk ->
            val message = MulticastMessage.builder()
                .addAllTokens(chunk)
                .setNotification(Notification.builder().setTitle(payload.title).setBody(payload.body).build())
                .putAllData(payload.data())
                .build()
            try {
                handleResponse(chunk, messaging.sendEachForMulticast(message))
            } catch (exception: FirebaseMessagingException) {
                log.warn("FCM multicast failed; retaining tokens for retry: tokenCount={}", chunk.size, exception)
            }
        }
    }

    private fun handleResponse(tokens: List<String>, response: BatchResponse) {
        response.responses.forEachIndexed { index, result ->
            val exception = result.exception ?: return@forEachIndexed
            if (exception.messagingErrorCode in INVALID_TOKEN_ERRORS) {
                deviceTokenRepository.deleteByToken(tokens[index])
            }
        }
    }

    private fun initializeMessaging(): FirebaseMessaging {
        val app = FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(firebaseOptions())
            ?: error("Firebase initialization returned no app while FCM is enabled")
        return FirebaseMessaging.getInstance(app)
    }

    private fun firebaseOptions(): FirebaseOptions {
        val credentials = properties.serviceAccountBase64
            ?.takeIf { it.isNotBlank() }
            ?.let { encoded ->
                val json = Base64.getDecoder().decode(encoded)
                ByteArrayInputStream(json).use(GoogleCredentials::fromStream)
            }
            ?: properties.serviceAccountLocation
            ?.takeIf { it.isNotBlank() }
            ?.let { FileInputStream(it).use(GoogleCredentials::fromStream) }
            ?: GoogleCredentials.getApplicationDefault()
        return FirebaseOptions.builder()
            .setCredentials(credentials)
            .apply { properties.projectId?.takeIf { it.isNotBlank() }?.let(::setProjectId) }
            .build()
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
