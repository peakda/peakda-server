package com.peakda.server.infrastructure.mail

import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.SendFailedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties

class SmtpEmailSenderTest {

    private val mailSender = mock(JavaMailSender::class.java)
    private val sender = SmtpEmailSender(mailSender, EmailProperties())

    init {
        `when`(mailSender.createMimeMessage()).thenAnswer { MimeMessage(Session.getInstance(Properties())) }
    }

    @Test
    fun `수신자 거부는 영구 실패로 보고 예외 어디에도 주소를 남기지 않는다`() {
        val rejected = SendFailedException(
            "550 5.1.1 <a@peakda.com> Recipient address rejected",
            null,
            emptyArray(),
            emptyArray(),
            arrayOf(InternetAddress("a@peakda.com")),
        )
        doThrow(MailSendException("send failed: a@peakda.com", rejected)).`when`(mailSender).send(any(MimeMessage::class.java))

        val e = catchThrowableOfType(EmailSendException::class.java) { sender.send(message("a@peakda.com")) }

        assertThat(e.permanent).isTrue()
        assertThat(e.cause).isNull()
        assertThat(e.toString()).doesNotContain("a@peakda.com")
    }

    @Test
    fun `연결 실패는 일시 오류로 본다`() {
        doThrow(MailSendException("Mail server connection failed")).`when`(mailSender).send(any(MimeMessage::class.java))

        val e = catchThrowableOfType(EmailSendException::class.java) { sender.send(message("a@peakda.com")) }

        assertThat(e.permanent).isFalse()
        assertThat(e.reason).isEqualTo("MailSendException")
    }

    @Test
    fun `주소 형식 오류는 영구 실패다`() {
        val e = catchThrowableOfType(EmailSendException::class.java) { sender.send(message("not an address@@")) }

        assertThat(e.permanent).isTrue()
        assertThat(e.toString()).doesNotContain("not an address")
    }

    private fun message(to: String) = EmailMessage(to = to, subject = "제목", text = "본문", html = "<p>본문</p>")
}
