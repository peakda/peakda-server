package com.peakda.server.domain.notification.repository

import com.peakda.server.domain.notification.entity.NoticeDispatch
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface NoticeDispatchRepository : JpaRepository<NoticeDispatch, Long> {

    @Query(
        value = """
            INSERT INTO notice_dispatches (notice_id, user_id, created_at)
            SELECT :noticeId, user_id, now()
            FROM unnest(CAST(:userIds AS bigint[])) AS user_id
            ON CONFLICT (notice_id, user_id) DO NOTHING
            RETURNING user_id
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(noticeId: Long, userIds: LongArray): List<Long>

    fun countByNoticeId(noticeId: Long): Long
}
