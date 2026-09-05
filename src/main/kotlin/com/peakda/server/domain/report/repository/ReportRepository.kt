package com.peakda.server.domain.report.repository

import com.peakda.server.domain.report.entity.Report
import com.peakda.server.domain.report.entity.ReportReason
import com.peakda.server.domain.report.entity.ReportStatus
import com.peakda.server.domain.report.entity.ReportTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ReportRepository : JpaRepository<Report, Long> {

    /**
     * 신고를 멱등하게 추가한다. 같은 사용자가 같은 대상을 이미 신고했으면 무시되므로
     * 동시 요청·중복 신고에서도 단일 행이 보장된다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO reports (reporter_id, target_type, target_id, reason, detail, created_at, updated_at)
            VALUES (:reporterId, :targetType, :targetId, :reason, :detail, now(), now())
            ON CONFLICT (reporter_id, target_type, target_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(reporterId: Long, targetType: String, targetId: Long, reason: String, detail: String?)

    @Query(
        value = """
            SELECT r.targetType AS targetType, r.targetId AS targetId, COUNT(r) AS reportCount,
                   MIN(r.createdAt) AS firstReportedAt, MAX(r.createdAt) AS lastReportedAt
            FROM Report r
            WHERE r.status = :status
            GROUP BY r.targetType, r.targetId
            ORDER BY COUNT(r) DESC, MAX(r.createdAt) DESC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT CONCAT(r.targetType, ':', r.targetId))
            FROM Report r
            WHERE r.status = :status
        """,
    )
    fun findTargetSummaries(
        @Param("status") status: ReportStatus,
        pageable: Pageable,
    ): Page<ReportTargetSummaryProjection>

    fun findByTargetTypeAndTargetIdOrderByIdDesc(
        targetType: ReportTargetType,
        targetId: Long,
    ): List<Report>

    @Query(
        """
            SELECT r.reason AS reason, COUNT(r) AS reportCount
            FROM Report r
            WHERE r.targetType = :targetType
              AND r.targetId = :targetId
            GROUP BY r.reason
            ORDER BY COUNT(r) DESC, r.reason ASC
        """,
    )
    fun findReasonDistribution(
        @Param("targetType") targetType: ReportTargetType,
        @Param("targetId") targetId: Long,
    ): List<ReportReasonCountProjection>

    fun existsByTargetTypeAndTargetId(targetType: ReportTargetType, targetId: Long): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            UPDATE Report r
               SET r.status = :status,
                   r.reviewedBy = :reviewedBy,
                   r.reviewedAt = :reviewedAt,
                   r.reviewMemo = :memo
             WHERE r.targetType = :targetType
               AND r.targetId = :targetId
               AND r.status = :pendingStatus
        """,
    )
    fun reviewPendingByTarget(
        @Param("targetType") targetType: ReportTargetType,
        @Param("targetId") targetId: Long,
        @Param("status") status: ReportStatus,
        @Param("reviewedBy") reviewedBy: Long,
        @Param("reviewedAt") reviewedAt: Instant,
        @Param("memo") memo: String?,
        @Param("pendingStatus") pendingStatus: ReportStatus,
    ): Int
}

interface ReportTargetSummaryProjection {
    fun getTargetType(): ReportTargetType
    fun getTargetId(): Long
    fun getReportCount(): Long
    fun getFirstReportedAt(): Instant
    fun getLastReportedAt(): Instant
}

interface ReportReasonCountProjection {
    fun getReason(): ReportReason
    fun getReportCount(): Long
}
