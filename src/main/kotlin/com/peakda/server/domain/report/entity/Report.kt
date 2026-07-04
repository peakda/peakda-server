package com.peakda.server.domain.report.entity

import com.peakda.server.common.persistence.BaseTimeEntity
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

/**
 * UGC 신고(P2-4). 운영 심사 대상 원천 데이터로만 적재하며, 관리자 처리 API는 V1 범위 밖이다.
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
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
