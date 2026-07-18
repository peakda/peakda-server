package com.peakda.server.infrastructure.push

import com.peakda.server.domain.notification.entity.DeviceToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/** 실제 FCM/APNs 어댑터로 교체할 지점. */
@Component
class LoggingPushSender : PushSender {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun send(tokens: List<DeviceToken>, payload: PushPayload) {
        log.info("Push stub: tokenCount={}, title={}", tokens.size, payload.title)
    }
}
