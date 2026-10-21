package com.peakda.server.infrastructure.mail

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * SMTP 접속 정보는 `spring.mail.*` 이 맡고, 여기는 발신자만 둔다. 사용 여부(`app.mail.enabled`)는
 * [SmtpEmailSender]·[LoggingEmailSender] 중 어느 쪽을 띄울지 정하는 조건으로만 쓴다.
 *
 * @property fromAddress 발송 서비스에 승인된 발신 주소 (OCI Email Delivery 의 Approved Sender)
 */
@ConfigurationProperties(prefix = "app.mail")
data class EmailProperties(
    val fromAddress: String = "no-reply@peakda.com",
    val fromName: String = "피크다",
)
