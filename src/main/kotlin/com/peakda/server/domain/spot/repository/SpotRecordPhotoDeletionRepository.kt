package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecordPhotoDeletion
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface SpotRecordPhotoDeletionRepository : JpaRepository<SpotRecordPhotoDeletion, Long> {
    @Query(value = """
        SELECT * FROM spot_record_photo_deletions
        WHERE (status IN ('PENDING','RETRYABLE') AND next_attempt_at <= :now)
           OR (status = 'PROCESSING' AND lease_until <= :now)
        ORDER BY next_attempt_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    fun findNextForUpdate(now: Instant): SpotRecordPhotoDeletion?

    @Query("select d.status as status, count(d) as total from SpotRecordPhotoDeletion d where d.status <> com.peakda.server.domain.spot.entity.PhotoDeletionStatus.COMPLETED group by d.status")
    fun countOutstandingByStatus(): List<PhotoDeletionStatusCount>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from SpotRecordPhotoDeletion d where d.id = :id")
    fun findByIdForUpdate(id: Long): SpotRecordPhotoDeletion?
}
