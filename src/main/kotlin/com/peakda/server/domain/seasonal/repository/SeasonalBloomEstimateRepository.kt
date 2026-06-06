package com.peakda.server.domain.seasonal.repository

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

private const val SEASONAL_BLOOM_ESTIMATE_UPSERT_SQL = """
    INSERT INTO seasonal_bloom_estimates (
        attraction_id, bloom_category, base_date, status, confidence, chosen_estimator,
        peak_start_date, peak_end_date, peak_duration_days, gdd_ratio, evidence, created_at, updated_at
    ) VALUES (
        :#{#command.attractionId}, :#{#command.bloomCategory}, :#{#command.baseDate}, :#{#command.status},
        :#{#command.confidence}, :#{#command.chosenEstimator}, :#{#command.peakStartDate}, :#{#command.peakEndDate},
        :#{#command.peakDurationDays}, :#{#command.gddRatio}, :#{#command.evidence}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_seasonal_bloom_estimates_attraction_category_date DO UPDATE SET
        status = EXCLUDED.status,
        confidence = EXCLUDED.confidence,
        chosen_estimator = EXCLUDED.chosen_estimator,
        peak_start_date = EXCLUDED.peak_start_date,
        peak_end_date = EXCLUDED.peak_end_date,
        peak_duration_days = EXCLUDED.peak_duration_days,
        gdd_ratio = EXCLUDED.gdd_ratio,
        evidence = EXCLUDED.evidence,
        updated_at = now()
"""

interface SeasonalBloomEstimateRepository : JpaRepository<SeasonalBloomEstimate, Long> {

    fun findByAttractionIdAndBaseDate(attractionId: Long, baseDate: LocalDate): List<SeasonalBloomEstimate>

    fun findFirstByAttractionIdAndBloomCategoryOrderByBaseDateDesc(
        attractionId: Long,
        bloomCategory: BloomCategory,
    ): SeasonalBloomEstimate?

    /** 산출된 가장 최근 base_date. "현재 상태" 조회의 기준일이 된다 (없으면 null). */
    @Query("SELECT MAX(e.baseDate) FROM SeasonalBloomEstimate e")
    fun findLatestBaseDate(): LocalDate?

    fun findByBaseDateAndAttractionIdIn(
        baseDate: LocalDate,
        attractionIds: Collection<Long>,
    ): List<SeasonalBloomEstimate>

    fun findByBaseDateAndAttractionIdInAndBloomCategory(
        baseDate: LocalDate,
        attractionIds: Collection<Long>,
        bloomCategory: BloomCategory,
    ): List<SeasonalBloomEstimate>

    fun findByBaseDateAndStatus(
        baseDate: LocalDate,
        status: BloomStatus,
    ): List<SeasonalBloomEstimate>

    fun findByBaseDateAndStatusAndBloomCategory(
        baseDate: LocalDate,
        status: BloomStatus,
        bloomCategory: BloomCategory,
    ): List<SeasonalBloomEstimate>

    @Modifying
    @Query(value = SEASONAL_BLOOM_ESTIMATE_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: SeasonalBloomEstimateUpsertCommand): Int
}

/**
 * [SeasonalBloomEstimate] upsert 입력. enum 은 native 바인딩 모호성을 피하려 `name` 문자열로 전달한다.
 */
data class SeasonalBloomEstimateUpsertCommand(
    val attractionId: Long,
    val bloomCategory: String,
    val baseDate: LocalDate,
    val status: String,
    val confidence: Double,
    val chosenEstimator: String,
    val peakStartDate: LocalDate?,
    val peakEndDate: LocalDate?,
    val peakDurationDays: Int?,
    val gddRatio: Double?,
    val evidence: String?,
)
