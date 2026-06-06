package com.peakda.server.domain.seasonal.entity

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
import java.time.LocalDate

/**
 * Q1 산출물 — 특정 명소×카테고리의 산출일(base_date) 시점 개화 상태.
 *
 * UK `(attraction_id, bloom_category, base_date)` 로 매일 1행 누적되어 역대 타이밍 아카이브가 된다.
 * [peakStartDate]·[peakEndDate]·[peakDurationDays] 는 채택 추정기가 제시한 절정 구간(올해 만개 시기/만개지속일 UI).
 */
@Entity
@Table(
    name = "seasonal_bloom_estimates",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_seasonal_bloom_estimates_attraction_category_date",
            columnNames = ["attraction_id", "bloom_category", "base_date"],
        ),
    ],
    indexes = [
        Index(name = "ix_seasonal_bloom_estimates_base_date", columnList = "base_date"),
        Index(name = "ix_seasonal_bloom_estimates_status", columnList = "status"),
        Index(name = "ix_seasonal_bloom_estimates_category_date", columnList = "bloom_category, base_date"),
    ],
)
class SeasonalBloomEstimate(
    @Column(name = "attraction_id", nullable = false)
    val attractionId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_category", nullable = false, columnDefinition = "TEXT")
    val bloomCategory: BloomCategory,

    @Column(name = "base_date", nullable = false)
    val baseDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: BloomStatus,

    @Column(name = "confidence", nullable = false)
    var confidence: Double,

    @Enumerated(EnumType.STRING)
    @Column(name = "chosen_estimator", nullable = false, columnDefinition = "TEXT")
    var chosenEstimator: Estimator,

    @Column(name = "peak_start_date")
    var peakStartDate: LocalDate? = null,

    @Column(name = "peak_end_date")
    var peakEndDate: LocalDate? = null,

    @Column(name = "peak_duration_days")
    var peakDurationDays: Int? = null,

    @Column(name = "gdd_ratio")
    var gddRatio: Double? = null,

    @Column(name = "evidence", columnDefinition = "TEXT")
    var evidence: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
