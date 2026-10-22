package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.entity.LaunchAlertMailTemplate
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.exception.LaunchAlertDispatchInProgressException
import com.peakda.server.domain.launchalert.repository.LaunchAlertDispatchRepository
import com.peakda.server.domain.launchalert.repository.LaunchAlertMailTemplateRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.Optional

class LaunchAlertMailTemplateServiceTest {

    private val templateRepository = mock(LaunchAlertMailTemplateRepository::class.java)
    private val dispatchRepository = mock(LaunchAlertDispatchRepository::class.java)
    private val auditRecorder = mock(AdminAuditRecorder::class.java)
    private val service = LaunchAlertMailTemplateService(templateRepository, dispatchRepository, LaunchAlertMailComposer(), auditRecorder)

    private val template = LaunchAlertMailTemplate(LaunchAlertPlatform.IOS, "제목", "큰 제목", "본문", "버튼", "하단")
        .also { ReflectionTestUtils.setField(it, "updatedAt", Instant.parse("2026-10-22T00:00:00Z")) }

    @BeforeEach
    fun setUp() {
        `when`(templateRepository.findById(LaunchAlertPlatform.IOS)).thenReturn(Optional.of(template))
        `when`(templateRepository.findByIdForUpdate(LaunchAlertPlatform.IOS)).thenReturn(template)
    }

    @Test
    fun `문구를 다듬어 저장하고 수정 사실을 감사 로그에 남긴다`() {
        val response = service.update(ADMIN_ID, command(subject = "  새 제목 ", body = "\n새 본문\n"))

        assertThat(response.subject).isEqualTo("새 제목")
        assertThat(response.body).isEqualTo("새 본문")
        assertThat(template.updatedBy).isEqualTo(ADMIN_ID)
        verify(auditRecorder).record(
            RecordAdminAuditCommand(
                adminId = ADMIN_ID,
                action = AdminAuditAction.LAUNCH_ALERT_MAIL_TEMPLATE_UPDATE,
                targetType = AdminAuditTargetType.LAUNCH_ALERT,
                targetId = 0,
                memo = "IOS",
            ),
        )
    }

    @Test
    fun `보내는 중에는 문구를 고칠 수 없다`() {
        `when`(dispatchRepository.existsByPlatformAndStatus(LaunchAlertPlatform.IOS, LaunchAlertDispatchStatus.DISPATCHING))
            .thenReturn(true)

        assertThatThrownBy { service.update(ADMIN_ID, command()) }
            .isInstanceOf(LaunchAlertDispatchInProgressException::class.java)
        assertThat(template.subject).isEqualTo("제목")
        verifyNoInteractions(auditRecorder)
    }

    @Test
    fun `미리보기는 App Store 주소가 아니면 기본 주소를 버튼에 건다`() {
        val html = service.previewHtml(LaunchAlertPlatform.IOS, "javascript:alert(1)")

        assertThat(html).contains("href=\"https://apps.apple.com/\"").doesNotContain("javascript:")
    }

    @Test
    fun `미리보기는 넘긴 App Store 주소를 버튼에 건다`() {
        val html = service.previewHtml(LaunchAlertPlatform.IOS, "https://apps.apple.com/kr/app/id1")

        assertThat(html).contains("href=\"https://apps.apple.com/kr/app/id1\"").contains(">큰 제목</td>")
    }

    private fun command(subject: String = "제목", body: String = "본문") = UpdateLaunchAlertMailTemplateCommand(
        platform = LaunchAlertPlatform.IOS,
        content = LaunchAlertMailContent(subject, "큰 제목", body, "버튼", "하단"),
    )

    companion object {
        private const val ADMIN_ID = 1L
    }
}
