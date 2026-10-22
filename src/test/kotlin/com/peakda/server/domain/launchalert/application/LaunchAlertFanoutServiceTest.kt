package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatch
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.entity.LaunchAlertSubscription
import com.peakda.server.domain.launchalert.exception.LaunchAlertMailTemplateNotFoundException
import com.peakda.server.domain.launchalert.repository.LaunchAlertDispatchRepository
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import com.peakda.server.infrastructure.mail.EmailMessage
import com.peakda.server.infrastructure.mail.EmailSendException
import com.peakda.server.infrastructure.mail.EmailSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class LaunchAlertFanoutServiceTest {

    private val dispatchRepository = mock(LaunchAlertDispatchRepository::class.java)
    private val subscriptionRepository = mock(LaunchAlertSubscriptionRepository::class.java)
    private val emailSender = ScriptedEmailSender()
    private val mailTemplateService = mock(LaunchAlertMailTemplateService::class.java).also {
        `when`(it.content(LaunchAlertPlatform.IOS)).thenReturn(LaunchAlertMailContent("제목", "큰 제목", "본문", "버튼", "하단"))
    }
    private val dispatch = LaunchAlertDispatch(LaunchAlertPlatform.IOS, STORE_URL, requestedBy = 1L)
        .also { ReflectionTestUtils.setField(it, "id", DISPATCH_ID) }

    private val service = LaunchAlertFanoutService(
        dispatchRepository = dispatchRepository,
        subscriptionRepository = subscriptionRepository,
        mailComposer = LaunchAlertMailComposer(),
        mailTemplateService = mailTemplateService,
        emailSender = emailSender,
        properties = LaunchAlertProperties(dispatch = LaunchAlertProperties.Dispatch(maxConsecutiveFailures = 2)),
        transactionManager = NoOpTransactionManager(),
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
        pause = {},
    )

    @BeforeEach
    fun setUp() {
        `when`(dispatchRepository.findFirstByStatusOrderByIdAsc(LaunchAlertDispatchStatus.DISPATCHING)).thenReturn(dispatch)
        `when`(dispatchRepository.findById(DISPATCH_ID)).thenReturn(Optional.of(dispatch))
    }

    @Test
    fun `보낸 신청은 지우고 실패한 신청은 남긴 채 커서를 넘기고 묶음이 차지 않으면 끝낸다`() {
        givenBatch(subscription(1L, "a@peakda.com"), subscription(2L, "b@peakda.com"), subscription(3L, "c@peakda.com"))
        emailSender.transientFailFor += "b@peakda.com"

        val sent = service.dispatchNextBatch()

        assertThat(sent).isEqualTo(2)
        verify(subscriptionRepository).deleteSent(1L)
        verify(subscriptionRepository, never()).deleteSent(2L)
        verify(subscriptionRepository).deleteSent(3L)
        assertThat(dispatch.cursorId).isEqualTo(3L)
        assertThat(dispatch.sentCount).isEqualTo(2)
        assertThat(dispatch.failedCount).isEqualTo(1)
        assertThat(dispatch.status).isEqualTo(LaunchAlertDispatchStatus.COMPLETED)
        assertThat(dispatch.completedAt).isEqualTo(NOW)
        assertThat(emailSender.sent.first().text).contains(STORE_URL)
    }

    @Test
    fun `보내기 전에 커서를 옮겨 둔다`() {
        givenBatch(subscription(1L, "a@peakda.com"))
        emailSender.onSend = { assertThat(dispatch.cursorId).isEqualTo(1L) }

        service.dispatchNextBatch()

        assertThat(emailSender.sent).hasSize(1)
    }

    @Test
    fun `묶음이 가득 차면 끝내지 않고 다음 실행에서 이어 보낸다`() {
        givenBatch(*(1L..LaunchAlertFanoutService.BATCH_SIZE.toLong()).map { subscription(it, "u$it@peakda.com") }.toTypedArray())

        service.dispatchNextBatch()

        assertThat(dispatch.status).isEqualTo(LaunchAlertDispatchStatus.DISPATCHING)
        assertThat(dispatch.cursorId).isEqualTo(LaunchAlertFanoutService.BATCH_SIZE.toLong())
    }

    @Test
    fun `일시 오류가 연속되면 SMTP 장애로 보고 중단한다`() {
        givenBatch(subscription(1L, "a@peakda.com"), subscription(2L, "b@peakda.com"), subscription(3L, "c@peakda.com"))
        emailSender.transientFailFor += listOf("a@peakda.com", "b@peakda.com")

        service.dispatchNextBatch()

        assertThat(dispatch.status).isEqualTo(LaunchAlertDispatchStatus.FAILED)
        assertThat(dispatch.cursorId).isEqualTo(2L)
        assertThat(emailSender.sent).isEmpty()
        verify(subscriptionRepository, never()).deleteSent(anyLong())
    }

    @Test
    fun `주소 문제로 실패한 신청은 바로 지우고 장애 판단에 넣지 않은 채 다음 사람에게 계속 보낸다`() {
        givenBatch(subscription(1L, "a@peakda.com"), subscription(2L, "b@peakda.com"), subscription(3L, "c@peakda.com"))
        emailSender.permanentFailFor += listOf("a@peakda.com", "b@peakda.com")

        service.dispatchNextBatch()

        assertThat(dispatch.status).isEqualTo(LaunchAlertDispatchStatus.COMPLETED)
        assertThat(dispatch.failedCount).isEqualTo(2)
        assertThat(dispatch.sentCount).isEqualTo(1)
        verify(subscriptionRepository).deleteSent(1L)
        verify(subscriptionRepository).deleteSent(2L)
        verify(subscriptionRepository).deleteSent(3L)
    }

    @Test
    fun `관리자가 멈추면 다음 수신자부터 보내지 않는다`() {
        givenBatch(subscription(1L, "a@peakda.com"), subscription(2L, "b@peakda.com"))
        emailSender.onSend = { dispatch.cancel(NOW) }

        val sent = service.dispatchNextBatch()

        assertThat(sent).isEqualTo(1)
        assertThat(dispatch.status).isEqualTo(LaunchAlertDispatchStatus.CANCELED)
        assertThat(dispatch.sentCount).isEqualTo(1)
        verify(subscriptionRepository, never()).deleteSent(2L)
    }

    @Test
    fun `메일 문구가 없으면 보내지 않고 중단한다`() {
        givenBatch(subscription(1L, "a@peakda.com"))
        `when`(mailTemplateService.content(LaunchAlertPlatform.IOS)).thenThrow(LaunchAlertMailTemplateNotFoundException())

        service.dispatchNextBatch()

        assertThat(dispatch.status).isEqualTo(LaunchAlertDispatchStatus.FAILED)
        assertThat(emailSender.sent).isEmpty()
    }

    @Test
    fun `메일이 꺼져 있으면 아무것도 지우지 않고 중단한다`() {
        emailSender.available = false

        service.dispatchNextBatch()

        assertThat(dispatch.status).isEqualTo(LaunchAlertDispatchStatus.FAILED)
        verify(subscriptionRepository, never()).deleteSent(anyLong())
    }

    private fun givenBatch(vararg subscriptions: LaunchAlertSubscription) {
        `when`(subscriptionRepository.findTop100ByPlatformAndIdGreaterThanOrderByIdAsc(LaunchAlertPlatform.IOS, 0L))
            .thenReturn(subscriptions.toList())
    }

    private fun subscription(id: Long, email: String) =
        LaunchAlertSubscription(LaunchAlertPlatform.IOS, email, "v1", NOW)
            .also { ReflectionTestUtils.setField(it, "id", id) }

    private class ScriptedEmailSender : EmailSender {
        override var available: Boolean = true
        val transientFailFor = mutableSetOf<String>()
        val permanentFailFor = mutableSetOf<String>()
        val sent = mutableListOf<EmailMessage>()
        var onSend: () -> Unit = {}

        override fun send(message: EmailMessage) {
            if (message.to in transientFailFor) throw EmailSendException("MailSendException", permanent = false)
            if (message.to in permanentFailFor) throw EmailSendException("AddressException", permanent = true)
            onSend()
            sent += message
        }
    }

    private class NoOpTransactionManager : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

        override fun commit(status: TransactionStatus) = Unit

        override fun rollback(status: TransactionStatus) = Unit
    }

    companion object {
        private const val DISPATCH_ID = 3L
        private const val STORE_URL = "https://apps.apple.com/kr/app/id0000000000"
        private val NOW = Instant.parse("2026-11-02T01:00:00Z")
    }
}
