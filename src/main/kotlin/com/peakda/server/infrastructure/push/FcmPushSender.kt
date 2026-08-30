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
import java.time.Duration
import kotlin.math.pow

/** Firebase Admin SDK FCM adapter. It is created only when FCM delivery is enabled. */
@Component
@ConditionalOnProperty(prefix = "app.push", name = ["enabled"], havingValue = "true")
class FcmPushSender(
    private val messaging: FirebaseMessaging,
    private val deviceTokenService: DeviceTokenService,
    private val properties: FcmProperties,
) : PushSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(tokens: List<DeviceToken>, payload: PushPayload) {
        if (tokens.isEmpty()) return

        tokens.map { it.token }.distinct().chunked(MAX_MULTICAST_SIZE).forEach { chunk ->
            sendChunk(chunk, payload)
        }
    }

    /**
     * 한 묶음을 발송하고, 일시 장애로 실패한 토큰만 백오프를 두고 다시 보낸다.
     * 무효 토큰은 시도마다 즉시 지우지만 재시도 한도를 넘긴 토큰은 남긴다 —
     * 일시 장애를 토큰 폐기로 오인하면 멀쩡한 기기가 알림을 영구히 받지 못한다.
     */
    private fun sendChunk(tokens: List<String>, payload: PushPayload) {
        var pending = tokens
        var attempt = 0
        while (true) {
            val outcome = dispatch(pending, payload)
            deviceTokenService.deleteInvalid(outcome.invalid)
            if (outcome.retryable.isEmpty()) return
            if (attempt == properties.retry.maxAttempts) {
                log.warn("FCM retry exhausted; retaining tokens: tokenCount={}", outcome.retryable.size)
                return
            }
            if (!sleepBeforeRetry(backoff(attempt))) return
            attempt++
            pending = outcome.retryable
        }
    }

    private fun dispatch(tokens: List<String>, payload: PushPayload): Outcome {
        val message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(Notification.builder().setTitle(payload.title).setBody(payload.body).build())
            .putAllData(payload.data())
            .build()
        return try {
            classify(tokens, messaging.sendEachForMulticast(message))
        } catch (exception: FirebaseMessagingException) {
            // 묶음 전체가 전송되지 못해 개별 결과가 없다. 전량을 재시도 대상으로 본다.
            log.warn("FCM multicast call failed: tokenCount={}", tokens.size, exception)
            Outcome(invalid = emptyList(), retryable = tokens)
        }
    }

    private fun classify(tokens: List<String>, response: BatchResponse): Outcome {
        val invalid = mutableListOf<String>()
        val retryable = mutableListOf<String>()
        response.responses.forEachIndexed { index, result ->
            val exception = result.exception ?: return@forEachIndexed
            val errorCode = exception.messagingErrorCode
            when {
                errorCode == null -> log.warn("FCM delivery failed without an error code", exception)
                errorCode in INVALID_TOKEN_ERRORS -> invalid += tokens[index]
                errorCode in RETRYABLE_ERRORS -> retryable += tokens[index]
                else -> log.warn("FCM delivery failed: errorCode={}", errorCode)
            }
        }
        return Outcome(invalid, retryable)
    }

    private fun backoff(attempt: Int): Duration {
        val factor = properties.retry.multiplier.pow(attempt)
        return Duration.ofMillis((properties.retry.initialBackoff.toMillis() * factor).toLong())
    }

    /** 인터럽트되면 재시도를 포기한다. 종료 신호를 무시하고 계속 대기하지 않기 위해서다. */
    private fun sleepBeforeRetry(duration: Duration): Boolean =
        try {
            Thread.sleep(duration)
            true
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("FCM retry interrupted; retaining tokens", exception)
            false
        }

    private data class Outcome(val invalid: List<String>, val retryable: List<String>)

    companion object {
        private const val MAX_MULTICAST_SIZE = 500

        /** 기기가 더 이상 이 토큰을 쓰지 않는다는 뜻이라 즉시 지운다. */
        private val INVALID_TOKEN_ERRORS = setOf(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
        )

        /** FCM 측 일시 장애. 토큰은 멀쩡하므로 유지하고 다시 보낸다. */
        private val RETRYABLE_ERRORS = setOf(
            MessagingErrorCode.INTERNAL,
            MessagingErrorCode.UNAVAILABLE,
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
