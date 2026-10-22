package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatch
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.entity.LaunchAlertMailTemplate
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.exception.LaunchAlertDispatchInProgressException
import com.peakda.server.domain.launchalert.exception.LaunchAlertDispatchNotCancelableException
import com.peakda.server.domain.launchalert.exception.LaunchAlertMailSendFailedException
import com.peakda.server.domain.launchalert.exception.LaunchAlertMailUnavailableException
import com.peakda.server.domain.launchalert.exception.LaunchAlertNoRecipientsException
import com.peakda.server.domain.launchalert.exception.LaunchAlertSubscriptionNotFoundException
import com.peakda.server.domain.launchalert.repository.LaunchAlertDispatchRepository
import com.peakda.server.domain.launchalert.repository.LaunchAlertMailTemplateRepository
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import com.peakda.server.infrastructure.mail.EmailMessage
import com.peakda.server.infrastructure.mail.EmailSendException
import com.peakda.server.infrastructure.mail.EmailSender
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.ZoneOffset
import java.time.Instant
import java.util.Optional

class LaunchAlertAdminServiceTest {

    private val subscriptionRepository = mock(LaunchAlertSubscriptionRepository::class.java)
    private val dispatchRepository = mock(LaunchAlertDispatchRepository::class.java)
    private val auditRecorder = mock(AdminAuditRecorder::class.java)
    private val emailSender = RecordingEmailSender()
    private val mailTemplateRepository = mock(LaunchAlertMailTemplateRepository::class.java).also {
        `when`(it.findByIdForUpdate(LaunchAlertPlatform.IOS))
            .thenReturn(LaunchAlertMailTemplate(LaunchAlertPlatform.IOS, "제목", "큰 제목", "본문", "버튼", "하단"))
    }
    private val mailTemplateService = mock(LaunchAlertMailTemplateService::class.java).also {
        `when`(it.content(LaunchAlertPlatform.IOS)).thenReturn(LaunchAlertMailContent("제목", "큰 제목", "본문", "버튼", "하단"))
    }

    private val service = LaunchAlertAdminService(
        subscriptionRepository = subscriptionRepository,
        dispatchRepository = dispatchRepository,
        mailComposer = LaunchAlertMailComposer(),
        mailTemplateService = mailTemplateService,
        mailTemplateRepository = mailTemplateRepository,
        emailSender = emailSender,
        adminAuditRecorder = auditRecorder,
        schedulerProperties = SchedulerProperties(enabled = true),
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
    )

    @Test
    fun `발송을 요청하면 발송 건을 만들고 감사 로그에는 건수만 남긴다`() {
        `when`(subscriptionRepository.countByPlatform(LaunchAlertPlatform.IOS)).thenReturn(2L)
        `when`(dispatchRepository.saveAndFlush(any(LaunchAlertDispatch::class.java))).thenAnswer {
            (it.arguments[0] as LaunchAlertDispatch).also { dispatch ->
                ReflectionTestUtils.setField(dispatch, "id", 3L)
                ReflectionTestUtils.setField(dispatch, "createdAt", NOW)
            }
        }

        val response = service.requestDispatch(ADMIN_ID, RequestLaunchAlertDispatchCommand(LaunchAlertPlatform.IOS, STORE_URL))

        assertThat(response.status).isEqualTo(LaunchAlertDispatchStatus.DISPATCHING)
        assertThat(response.storeUrl).isEqualTo(STORE_URL)
        verify(auditRecorder).record(audit(AdminAuditAction.LAUNCH_ALERT_DISPATCH, 3L, "IOS 2건"))
    }

    @Test
    fun `메일이 꺼져 있으면 발송을 시작하지 않는다`() {
        emailSender.available = false

        assertThatThrownBy {
            service.requestDispatch(ADMIN_ID, RequestLaunchAlertDispatchCommand(LaunchAlertPlatform.IOS, STORE_URL))
        }.isInstanceOf(LaunchAlertMailUnavailableException::class.java)
        verifyNoInteractions(dispatchRepository, auditRecorder)
    }

    @Test
    fun `발송 잡이 꺼져 있으면 발송을 시작하지 않는다`() {
        val serviceWithJobOff = LaunchAlertAdminService(
            subscriptionRepository, dispatchRepository, LaunchAlertMailComposer(), mailTemplateService, mailTemplateRepository, emailSender, auditRecorder,
            SchedulerProperties(enabled = false),
        )

        assertThatThrownBy {
            serviceWithJobOff.requestDispatch(ADMIN_ID, RequestLaunchAlertDispatchCommand(LaunchAlertPlatform.IOS, STORE_URL))
        }.isInstanceOf(LaunchAlertMailUnavailableException::class.java)
    }

    @Test
    fun `동시에 들어온 요청이 먼저 만들었으면 409 로 알린다`() {
        `when`(subscriptionRepository.countByPlatform(LaunchAlertPlatform.IOS)).thenReturn(2L)
        `when`(dispatchRepository.saveAndFlush(any(LaunchAlertDispatch::class.java)))
            .thenThrow(DataIntegrityViolationException("uk_launch_alert_dispatches_platform_dispatching"))

        assertThatThrownBy {
            service.requestDispatch(ADMIN_ID, RequestLaunchAlertDispatchCommand(LaunchAlertPlatform.IOS, STORE_URL))
        }.isInstanceOf(LaunchAlertDispatchInProgressException::class.java)
    }

    @Test
    fun `보내는 중인 발송을 멈추고 끝난 발송은 멈출 수 없다`() {
        val dispatch = LaunchAlertDispatch(LaunchAlertPlatform.IOS, STORE_URL, requestedBy = ADMIN_ID).also {
            ReflectionTestUtils.setField(it, "id", 3L)
            ReflectionTestUtils.setField(it, "createdAt", NOW)
        }
        `when`(dispatchRepository.findById(3L)).thenReturn(Optional.of(dispatch))

        val response = service.cancelDispatch(ADMIN_ID, 3L)

        assertThat(response.status).isEqualTo(LaunchAlertDispatchStatus.CANCELED)
        assertThat(response.completedAt).isEqualTo(NOW)
        verify(auditRecorder).record(audit(AdminAuditAction.LAUNCH_ALERT_DISPATCH_CANCEL, 3L, "IOS"))
        assertThatThrownBy { service.cancelDispatch(ADMIN_ID, 3L) }
            .isInstanceOf(LaunchAlertDispatchNotCancelableException::class.java)
    }

    @Test
    fun `이미 보내는 중이면 새 발송을 만들지 않는다`() {
        `when`(dispatchRepository.existsByPlatformAndStatus(LaunchAlertPlatform.IOS, LaunchAlertDispatchStatus.DISPATCHING))
            .thenReturn(true)

        assertThatThrownBy {
            service.requestDispatch(ADMIN_ID, RequestLaunchAlertDispatchCommand(LaunchAlertPlatform.IOS, STORE_URL))
        }.isInstanceOf(LaunchAlertDispatchInProgressException::class.java)
    }

    @Test
    fun `남은 신청이 없으면 발송을 만들지 않는다`() {
        `when`(subscriptionRepository.countByPlatform(LaunchAlertPlatform.IOS)).thenReturn(0L)

        assertThatThrownBy {
            service.requestDispatch(ADMIN_ID, RequestLaunchAlertDispatchCommand(LaunchAlertPlatform.IOS, STORE_URL))
        }.isInstanceOf(LaunchAlertNoRecipientsException::class.java)
    }

    @Test
    fun `테스트 메일은 지정한 주소로 스토어 주소를 넣어 보낸다`() {
        service.sendTestMail(ADMIN_ID, SendLaunchAlertTestMailCommand(LaunchAlertPlatform.IOS, STORE_URL, " admin@peakda.com "))

        val message = emailSender.sent.single()
        assertThat(message.to).isEqualTo("admin@peakda.com")
        assertThat(message.text).contains(STORE_URL)
        verify(auditRecorder).record(audit(AdminAuditAction.LAUNCH_ALERT_TEST_SEND, 0L, "IOS"))
    }

    @Test
    fun `테스트 메일을 보내지 못해도 시도는 감사 로그에 남기고 실패를 알린다`() {
        emailSender.failing = true

        assertThatThrownBy {
            service.sendTestMail(ADMIN_ID, SendLaunchAlertTestMailCommand(LaunchAlertPlatform.IOS, STORE_URL, "admin@peakda.com"))
        }.isInstanceOf(LaunchAlertMailSendFailedException::class.java)
        verify(auditRecorder).record(audit(AdminAuditAction.LAUNCH_ALERT_TEST_SEND, 0L, "IOS"))
    }

    @Test
    fun `없는 신청은 삭제할 수 없다`() {
        `when`(subscriptionRepository.findById(9L)).thenReturn(Optional.empty())

        assertThatThrownBy { service.delete(ADMIN_ID, 9L) }
            .isInstanceOf(LaunchAlertSubscriptionNotFoundException::class.java)
        verifyNoInteractions(auditRecorder)
    }

    private fun audit(action: AdminAuditAction, targetId: Long, memo: String) =
        RecordAdminAuditCommand(
            adminId = ADMIN_ID,
            action = action,
            targetType = AdminAuditTargetType.LAUNCH_ALERT,
            targetId = targetId,
            memo = memo,
        )

    private class RecordingEmailSender : EmailSender {
        override var available: Boolean = true
        var failing: Boolean = false
        val sent = mutableListOf<EmailMessage>()

        override fun send(message: EmailMessage) {
            if (failing) throw EmailSendException("MailSendException", permanent = false)
            sent += message
        }
    }

    companion object {
        private const val ADMIN_ID = 1L
        private const val STORE_URL = "https://apps.apple.com/kr/app/id0000000000"
        private val NOW = Instant.parse("2026-11-02T01:00:00Z")
    }
}
