package com.peakda.server.domain.notification.repository

import com.peakda.server.domain.notification.entity.BloomTimingAlert
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface BloomTimingAlertRepository : JpaRepository<BloomTimingAlert, Long> {

    fun deleteByUserId(userId: Long)

    /**
     * 만개 임박 알림 발송 키를 멱등하게 기록한다. 새 키면 1, 이미 발송한 키면 0을 반환한다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO bloom_timing_alerts
                (user_id, spot_id, bloom_category, peak_year, peak_start_date, created_at, updated_at)
            VALUES (:userId, :spotId, :bloomCategory, :peakYear, :peakStartDate, now(), now())
            ON CONFLICT ON CONSTRAINT uk_bloom_timing_alerts_user_spot_category_year DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        userId: Long,
        spotId: Long,
        bloomCategory: String,
        peakYear: Int,
        peakStartDate: LocalDate,
    ): Int
}
