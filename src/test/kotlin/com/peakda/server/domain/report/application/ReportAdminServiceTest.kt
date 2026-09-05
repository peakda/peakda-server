package com.peakda.server.domain.report.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.report.entity.ReportStatus
import com.peakda.server.domain.report.entity.ReportTargetType
import com.peakda.server.domain.report.exception.ReportAlreadyReviewedException
import com.peakda.server.domain.report.repository.ReportRepository
import com.peakda.server.domain.spot.application.SpotRecordModerationService
import com.peakda.server.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

class ReportAdminServiceTest {

    private val reportRepository = mock(ReportRepository::class.java)
    private val moderationService = mock(SpotRecordModerationService::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val auditRecorder = mock(AdminAuditRecorder::class.java)
    private val service = ReportAdminService(
        reportRepository,
        moderationService,
        userRepository,
        auditRecorder,
    )

    @Test
    fun `숨김 처리는 대기 신고를 한 번에 전이하고 감사 로그 두 건을 남긴다`() {
        `when`(
            reportRepository.reviewPendingByTarget(
                eqValue(ReportTargetType.SPOT_RECORD),
                eqValue(TARGET_ID),
                eqValue(ReportStatus.RESOLVED),
                eqValue(ADMIN_ID),
                anyInstant(),
                eqValue(MEMO),
                eqValue(ReportStatus.PENDING),
            ),
        ).thenReturn(3)

        service.review(
            ReviewReportTargetCommand(
                adminId = ADMIN_ID,
                targetType = ReportTargetType.SPOT_RECORD,
                targetId = TARGET_ID,
                action = ReportReviewAction.RESOLVE_HIDE,
                memo = MEMO,
            ),
        )

        verify(moderationService).hide(TARGET_ID)
        verify(reportRepository).reviewPendingByTarget(
            eqValue(ReportTargetType.SPOT_RECORD),
            eqValue(TARGET_ID),
            eqValue(ReportStatus.RESOLVED),
            eqValue(ADMIN_ID),
            anyInstant(),
            eqValue(MEMO),
            eqValue(ReportStatus.PENDING),
        )
        val captor = ArgumentCaptor.forClass(RecordAdminAuditCommand::class.java)
        verify(auditRecorder, times(2)).record(captureCommand(captor))
        assertThat(captor.allValues.map { it.action })
            .containsExactly(AdminAuditAction.REPORT_RESOLVE_HIDE, AdminAuditAction.SPOT_RECORD_HIDE)
    }

    @Test
    fun `일괄 전이 결과가 0이고 신고가 존재하면 이미 심사된 예외를 던진다`() {
        `when`(
            reportRepository.reviewPendingByTarget(
                eqValue(ReportTargetType.SPOT_RECORD),
                eqValue(TARGET_ID),
                eqValue(ReportStatus.RESOLVED),
                eqValue(ADMIN_ID),
                anyInstant(),
                isNull(),
                eqValue(ReportStatus.PENDING),
            ),
        ).thenReturn(0)
        `when`(
            reportRepository.existsByTargetTypeAndTargetId(ReportTargetType.SPOT_RECORD, TARGET_ID),
        ).thenReturn(true)

        assertThatThrownBy {
            service.review(
                ReviewReportTargetCommand(
                    adminId = ADMIN_ID,
                    targetType = ReportTargetType.SPOT_RECORD,
                    targetId = TARGET_ID,
                    action = ReportReviewAction.RESOLVE_KEEP,
                    memo = null,
                ),
            )
        }.isInstanceOf(ReportAlreadyReviewedException::class.java)

        verify(auditRecorder, never()).record(anyAuditCommand())
    }

    private fun anyInstant(): Instant = any(Instant::class.java) ?: Instant.EPOCH

    private fun <T> eqValue(value: T): T = eq(value) ?: value

    private fun captureCommand(captor: ArgumentCaptor<RecordAdminAuditCommand>): RecordAdminAuditCommand =
        captor.capture() ?: auditCommand()

    private fun anyAuditCommand(): RecordAdminAuditCommand =
        any(RecordAdminAuditCommand::class.java) ?: auditCommand()

    private fun auditCommand(): RecordAdminAuditCommand =
        RecordAdminAuditCommand(
            adminId = ADMIN_ID,
            action = AdminAuditAction.REPORT_RESOLVE_KEEP,
            targetType = AdminAuditTargetType.SPOT_RECORD,
            targetId = TARGET_ID,
        )

    companion object {
        private const val ADMIN_ID = 7L
        private const val TARGET_ID = 1024L
        private const val MEMO = "정책 위반 게시글"
    }
}
