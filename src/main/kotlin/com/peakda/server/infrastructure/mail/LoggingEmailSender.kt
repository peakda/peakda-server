package com.peakda.server.infrastructure.mail

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.stereotype.Component

/** 메일 발송이 꺼진 환경(local·test)용. 보내지 않고 제목만 남긴다. */
@Component
@ConditionalOnBooleanProperty("app.mail.enabled", havingValue = false, matchIfMissing = true)
class LoggingEmailSender : EmailSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override val available: Boolean = false

    override fun send(message: EmailMessage) {
        log.info("Email stub: subject={}", message.subject)
    }
}
