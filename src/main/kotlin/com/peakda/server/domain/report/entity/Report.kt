package com.peakda.server.domain.report.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import com.peakda.server.domain.report.exception.ReportAlreadyReviewedException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

/**
 * UGC 신고(P2-4). 같은 대상의 대기 신고들은 관리자 심사에서 한 번에 처리된다.
 */
@Entity
@Table(
    name = "reports",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_reports_reporter_target",
            columnNames = ["reporter_id", "target_type", "target_id"],
        ),
    ],
    indexes = [
        Index(name = "ix_reports_target", columnList = "target_type,target_id"),
        Index(name = "ix_reports_status_created_at", columnList = "status,created_at DESC"),
    ],
)
class Report(
    @Column(name = "reporter_id", nullable = false)
    val reporterId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, columnDefinition = "TEXT")
    val targetType: ReportTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    val reason: ReportReason,

    @Column(name = "detail", columnDefinition = "TEXT")
    val detail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: ReportStatus = ReportStatus.PENDING,

    @Column(name = "reviewed_by")
    var reviewedBy: Long? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "review_memo", columnDefinition = "TEXT")
    var reviewMemo: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    fun resolve(reviewedBy: Long, reviewedAt: Instant, reviewMemo: String?) {
        review(ReportStatus.RESOLVED, reviewedBy, reviewedAt, reviewMemo)
    }

    fun dismiss(reviewedBy: Long, reviewedAt: Instant, reviewMemo: String?) {
        review(ReportStatus.DISMISSED, reviewedBy, reviewedAt, reviewMemo)
    }

    private fun review(status: ReportStatus, reviewedBy: Long, reviewedAt: Instant, reviewMemo: String?) {
        if (this.status != ReportStatus.PENDING) throw ReportAlreadyReviewedException()
        this.status = status
        this.reviewedBy = reviewedBy
        this.reviewedAt = reviewedAt
        this.reviewMemo = reviewMemo
    }
}
