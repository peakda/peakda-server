package com.peakda.server.domain.report.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.report.entity.Report
import com.peakda.server.domain.report.entity.ReportStatus
import com.peakda.server.domain.report.entity.ReportTargetType
import com.peakda.server.domain.report.exception.ReportActionNotSupportedException
import com.peakda.server.domain.report.exception.ReportAlreadyReviewedException
import com.peakda.server.domain.report.exception.ReportNotFoundException
import com.peakda.server.domain.report.presentation.response.ReportReviewItemResponse
import com.peakda.server.domain.report.presentation.response.ReportTargetDetailResponse
import com.peakda.server.domain.report.presentation.response.ReportTargetSummaryResponse
import com.peakda.server.domain.report.repository.ReportRepository
import com.peakda.server.domain.report.repository.ReportTargetSummaryProjection
import com.peakda.server.domain.spot.application.SpotRecordModerationService
import com.peakda.server.domain.spot.application.SpotRecordModerationSummary
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class ReportAdminService(
    private val reportRepository: ReportRepository,
    private val spotRecordModerationService: SpotRecordModerationService,
    private val userRepository: UserRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
) {

    @Transactional(readOnly = true)
    fun list(status: ReportStatus, pageable: Pageable): Page<ReportTargetSummaryResponse> {
        val summaries = reportRepository.findTargetSummaries(status, pageable)
        val targets = loadTargets(summaries.content.map { it.getTargetType() to it.getTargetId() })
        return summaries.map { summary -> summary.toResponse(targets.getValue(summary.getTargetType() to summary.getTargetId())) }
    }

    @Transactional(readOnly = true)
    fun detail(targetType: ReportTargetType, targetId: Long): ReportTargetDetailResponse {
        val reports = reportRepository.findByTargetTypeAndTargetIdOrderByIdDesc(targetType, targetId)
        if (reports.isEmpty()) throw ReportNotFoundException()

        val target = loadTargets(listOf(targetType to targetId)).getValue(targetType to targetId)
        val reporterNicknames = userRepository.findAllById(reports.map { it.reporterId }.distinct())
            .associate { requireNotNull(it.id) to it.nickname }
        val reasonDistribution = reportRepository.findReasonDistribution(targetType, targetId)
            .associate { it.getReason() to it.getReportCount() }

        return ReportTargetDetailResponse(
            targetType = targetType,
            targetId = targetId,
            reportCount = reports.size.toLong(),
            reasonDistribution = reasonDistribution,
            reports = reports.map { report -> report.toResponse(reporterNicknames[report.reporterId]) },
            targetSummary = target.summary,
            targetAuthorNickname = target.authorNickname,
            targetExists = target.exists,
        )
    }

    fun review(command: ReviewReportTargetCommand) {
        if (command.action == ReportReviewAction.RESOLVE_HIDE && command.targetType != ReportTargetType.SPOT_RECORD) {
            throw ReportActionNotSupportedException()
        }

        val nextStatus = when (command.action) {
            ReportReviewAction.RESOLVE_HIDE -> {
                spotRecordModerationService.hide(command.targetId)
                ReportStatus.RESOLVED
            }
            ReportReviewAction.RESOLVE_KEEP -> ReportStatus.RESOLVED
            ReportReviewAction.DISMISS -> ReportStatus.DISMISSED
        }
        val reviewed = reportRepository.reviewPendingByTarget(
            targetType = command.targetType,
            targetId = command.targetId,
            status = nextStatus,
            reviewedBy = command.adminId,
            reviewedAt = Instant.now(),
            memo = command.memo,
            pendingStatus = ReportStatus.PENDING,
        )
        if (reviewed == 0) {
            if (reportRepository.existsByTargetTypeAndTargetId(command.targetType, command.targetId)) {
                throw ReportAlreadyReviewedException()
            }
            throw ReportNotFoundException()
        }

        recordReportAudit(command)
        if (command.action == ReportReviewAction.RESOLVE_HIDE) {
            adminAuditRecorder.record(
                RecordAdminAuditCommand(
                    adminId = command.adminId,
                    action = AdminAuditAction.SPOT_RECORD_HIDE,
                    targetType = AdminAuditTargetType.SPOT_RECORD,
                    targetId = command.targetId,
                    memo = command.memo,
                ),
            )
        }
    }

    private fun loadTargets(
        targetKeys: List<Pair<ReportTargetType, Long>>,
    ): Map<Pair<ReportTargetType, Long>, ReportTargetContext> {
        val distinctKeys = targetKeys.distinct()
        if (distinctKeys.isEmpty()) return emptyMap()
        val spotRecordIds = distinctKeys
            .filter { it.first == ReportTargetType.SPOT_RECORD }
            .map { it.second }
        val spotRecords = spotRecordModerationService.summaries(spotRecordIds).associateBy { it.id }
        val userIds = spotRecords.values.map { it.userId }.distinct()
        val users = userRepository.findAllById(userIds).associateBy { requireNotNull(it.id) }

        return distinctKeys.associateWith { (targetType, targetId) ->
            when (targetType) {
                ReportTargetType.SPOT_RECORD -> spotRecords[targetId].toTargetContext(users)
            }
        }
    }

    private fun ReportTargetSummaryProjection.toResponse(target: ReportTargetContext): ReportTargetSummaryResponse =
        ReportTargetSummaryResponse(
            targetType = getTargetType(),
            targetId = getTargetId(),
            reportCount = getReportCount(),
            firstReportedAt = getFirstReportedAt(),
            lastReportedAt = getLastReportedAt(),
            targetSummary = target.summary,
            targetAuthorNickname = target.authorNickname,
            targetExists = target.exists,
        )

    private fun Report.toResponse(reporterNickname: String?): ReportReviewItemResponse =
        ReportReviewItemResponse(
            id = requireNotNull(id),
            reporterId = reporterId,
            reporterNickname = reporterNickname,
            reason = reason,
            detail = detail,
            status = status,
            createdAt = createdAt,
            reviewedBy = reviewedBy,
            reviewedAt = reviewedAt,
            reviewMemo = reviewMemo,
        )

    private fun SpotRecordModerationSummary?.toTargetContext(users: Map<Long, User>): ReportTargetContext {
        if (this == null) return ReportTargetContext.missing()
        val summary = listOfNotNull(visitedDate?.toString(), memo).joinToString(" · ").ifBlank { null }
        return ReportTargetContext(
            summary = summary,
            authorNickname = users[userId]?.nickname,
            exists = true,
        )
    }

    private fun recordReportAudit(command: ReviewReportTargetCommand) {
        val action = when (command.action) {
            ReportReviewAction.RESOLVE_HIDE -> AdminAuditAction.REPORT_RESOLVE_HIDE
            ReportReviewAction.RESOLVE_KEEP -> AdminAuditAction.REPORT_RESOLVE_KEEP
            ReportReviewAction.DISMISS -> AdminAuditAction.REPORT_DISMISS
        }
        val targetType = when (command.targetType) {
            ReportTargetType.SPOT_RECORD -> AdminAuditTargetType.SPOT_RECORD
        }
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = command.adminId,
                action = action,
                targetType = targetType,
                targetId = command.targetId,
                memo = command.memo,
            ),
        )
    }

}
