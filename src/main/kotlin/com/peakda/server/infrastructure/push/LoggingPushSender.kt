package com.peakda.server.infrastructure.push

import com.peakda.server.domain.notification.entity.DeviceToken
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/** Local/test fallback used while FCM delivery is disabled. */
@Component
@ConditionalOnProperty(prefix = "app.push", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class LoggingPushSender : PushSender {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun send(tokens: List<DeviceToken>, payload: PushPayload) {
        log.info("Push stub: tokenCount={}, title={}, type={}", tokens.size, payload.title, payload.type)
    }
}
