package com.peakda.server.domain.report.repository

import com.peakda.server.domain.report.entity.Report
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

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
}
