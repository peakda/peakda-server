package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.entity.LaunchAlertSubscription
import com.peakda.server.domain.launchalert.exception.LaunchAlertSubscriptionNotFoundException
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.never
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

class LaunchAlertAdminServiceTest {

    private val repository = mock(LaunchAlertSubscriptionRepository::class.java)
    private val auditRecorder = mock(AdminAuditRecorder::class.java)
    private val service = LaunchAlertAdminService(repository, auditRecorder, Clock.fixed(NOW, ZoneOffset.UTC))

    @Test
    fun `발송 대상은 신청 순 이메일을 주고 조회한 id 만 내려받음으로 표시한다`() {
        `when`(repository.findAllByPlatformOrderByIdAsc(LaunchAlertPlatform.IOS))
            .thenReturn(listOf(subscription(3L, "a@peakda.com"), subscription(7L, "b@peakda.com")))

        val response = service.recipients(ADMIN_ID, LaunchAlertPlatform.IOS)

        assertThat(response.emails).containsExactly("a@peakda.com", "b@peakda.com")
        assertThat(response.count).isEqualTo(2)
        verify(repository).markExported(longArrayOf(3L, 7L), NOW)
        verify(auditRecorder).record(audit(AdminAuditAction.LAUNCH_ALERT_EXPORT, 0L, "IOS 2건"))
    }

    @Test
    fun `대상이 없으면 표시하지 않고 빈 목록을 준다`() {
        `when`(repository.findAllByPlatformOrderByIdAsc(LaunchAlertPlatform.IOS)).thenReturn(emptyList())

        val response = service.recipients(ADMIN_ID, LaunchAlertPlatform.IOS)

        assertThat(response.emails).isEmpty()
        verify(repository, never()).markExported(longArrayOf(), NOW)
    }

    @Test
    fun `파기는 내려받은 신청을 지우고 지운 건수만 감사 로그에 남긴다`() {
        `when`(repository.deleteExported(LaunchAlertPlatform.IOS)).thenReturn(2)

        val response = service.purge(ADMIN_ID, LaunchAlertPlatform.IOS)

        assertThat(response.deletedCount).isEqualTo(2)
        verify(auditRecorder).record(audit(AdminAuditAction.LAUNCH_ALERT_PURGE, 0L, "IOS 2건"))
    }

    @Test
    fun `없는 신청은 삭제할 수 없다`() {
        `when`(repository.findById(9L)).thenReturn(Optional.empty())

        assertThatThrownBy { service.delete(ADMIN_ID, 9L) }
            .isInstanceOf(LaunchAlertSubscriptionNotFoundException::class.java)
        verifyNoInteractions(auditRecorder)
    }

    private fun subscription(id: Long, email: String) =
        LaunchAlertSubscription(
            platform = LaunchAlertPlatform.IOS,
            email = email,
            consentVersion = "v1",
            consentedAt = Instant.parse("2026-10-21T00:00:00Z"),
        ).also { ReflectionTestUtils.setField(it, "id", id) }

    private fun audit(action: AdminAuditAction, targetId: Long, memo: String) =
        RecordAdminAuditCommand(
            adminId = ADMIN_ID,
            action = action,
            targetType = AdminAuditTargetType.LAUNCH_ALERT,
            targetId = targetId,
            memo = memo,
        )

    companion object {
        private const val ADMIN_ID = 1L
        private val NOW = Instant.parse("2026-11-02T01:00:00Z")
    }
}
