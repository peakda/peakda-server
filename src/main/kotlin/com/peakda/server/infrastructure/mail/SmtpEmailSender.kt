package com.peakda.server.infrastructure.mail

import jakarta.mail.MessagingException
import jakarta.mail.SendFailedException
import jakarta.mail.internet.AddressException
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.mail.MailException
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/** SMTP 발송. prod 는 OCI Email Delivery 의 SMTP 엔드포인트로 보낸다. */
@Component
@ConditionalOnBooleanProperty("app.mail.enabled")
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    private val properties: EmailProperties,
) : EmailSender {

    override val available: Boolean = true

    override fun send(message: EmailMessage) {
        try {
            val mimeMessage = mailSender.createMimeMessage()
            MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name()).apply {
                setFrom(properties.fromAddress, properties.fromName)
                setTo(message.to)
                setSubject(message.subject)
                setText(message.text, message.html)
            }
            mailSender.send(mimeMessage)
        } catch (e: MailException) {
            throw EmailSendException(reasonOf(e), isPermanent(e))
        } catch (e: MessagingException) {
            throw EmailSendException(reasonOf(e), isPermanent(e))
        }
    }

    /** 원인 사슬의 클래스 이름만 잇는다. 메시지에는 수신 주소가 들어 있을 수 있다. */
    private fun reasonOf(e: Throwable): String =
        causes(e).joinToString(" > ") { it.javaClass.simpleName }

    /**
     * 주소 형식 오류이거나 서버가 이 수신자만 거부한 경우. 연결·인증 실패는 일시 장애로 본다.
     * [MailSendException] 은 메시지별 실패를 [MailSendException.getFailedMessages] 에 담는다.
     */
    private fun isPermanent(e: Throwable): Boolean {
        val chain = causes(e) + ((e as? MailSendException)?.failedMessages?.values?.flatMap { causes(it) } ?: emptyList())
        return chain.any { it is AddressException || (it is SendFailedException && !it.invalidAddresses.isNullOrEmpty()) }
    }

    private fun causes(e: Throwable): List<Throwable> =
        generateSequence(e) { it.cause?.takeIf { cause -> cause !== it } }.take(MAX_CAUSE_DEPTH).toList()

    companion object {
        private const val MAX_CAUSE_DEPTH = 10
    }
}
