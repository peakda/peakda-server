package com.peakda.server.domain.notification.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import com.peakda.server.domain.seasonal.entity.BloomCategory
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
import java.time.LocalDate

/**
 * P3-3 만개 임박 알림 중복 방지 로그.
 *
 * 동일 user×spot×카테고리×만개연도 조합을 한 번만 기록해 추정일이 바뀌어도 평생 1회 발송을 보장한다.
 */
@Entity
@Table(
    name = "bloom_timing_alerts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_bloom_timing_alerts_user_spot_category_year",
            columnNames = ["user_id", "spot_id", "bloom_category", "peak_year"],
        ),
    ],
    indexes = [Index(name = "ix_bloom_timing_alerts_user_id", columnList = "user_id")],
)
class BloomTimingAlert(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "spot_id", nullable = false)
    val spotId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_category", nullable = false, columnDefinition = "TEXT")
    val bloomCategory: BloomCategory,

    @Column(name = "peak_year", nullable = false)
    val peakYear: Int,

    @Column(name = "peak_start_date", nullable = false)
    val peakStartDate: LocalDate,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
