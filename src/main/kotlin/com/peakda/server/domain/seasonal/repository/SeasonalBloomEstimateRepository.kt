package com.peakda.server.domain.seasonal.repository

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    /**
     * 주어진 명소들의 특정 산출일 추정 중, 진행 중(status≠제외상태)이고
     * 만개 시작일이 [start, end] 창에 드는 것만.
     * 만개 임박 알림 후보 선별용. peak_start_date 가 null 이면 BETWEEN 에서 자연히 제외된다.
     */
    fun findByBaseDateAndAttractionIdInAndStatusNotAndPeakStartDateBetween(
        baseDate: LocalDate,
        attractionIds: Collection<Long>,
        status: BloomStatus,
        peakStartDateStart: LocalDate,
        peakStartDateEnd: LocalDate,
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

    /**
     * 산출일·상태가 일치하는 추정을 가진 명소 id 를 대표 신뢰도 높은 순으로 페이징한다.
     * 한 명소가 여러 카테고리로 여러 행을 갖기 때문에
     * 명소 단위로 묶어야 페이지 경계가 어긋나지 않는다.
     * 어떤 카테고리를 대표로 쓸지는 호출측(application)이 결정한다.
     *
     * 명소의 노출 여부는 이 레포가 알 수 없다.
     * 비노출 명소가 포함되면 실제 응답 건수가 페이지 메타보다 적을 수 있다.
     */
    @Query(
        value = """
            SELECT e.attractionId FROM SeasonalBloomEstimate e
            WHERE e.baseDate = :baseDate
              AND e.status = :status
            GROUP BY e.attractionId
            ORDER BY MAX(e.confidence) DESC, e.attractionId ASC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT e.attractionId) FROM SeasonalBloomEstimate e
            WHERE e.baseDate = :baseDate
              AND e.status = :status
        """,
    )
    fun findAttractionIdsByBaseDateAndStatus(
        @Param("baseDate") baseDate: LocalDate,
        @Param("status") status: BloomStatus,
        pageable: Pageable,
    ): Page<Long>

    /**
     * 산출일·상태·꽃 카테고리가 일치하는 추정을 가진 명소 id 를 대표 신뢰도 높은 순으로 페이징한다.
     * 명소의 노출 여부는 이 레포가 알 수 없다.
     * 비노출 명소가 포함되면 실제 응답 건수가 페이지 메타보다 적을 수 있다.
     */
    @Query(
        value = """
            SELECT e.attractionId FROM SeasonalBloomEstimate e
            WHERE e.baseDate = :baseDate
              AND e.status = :status
              AND e.bloomCategory = :bloomCategory
            GROUP BY e.attractionId
            ORDER BY MAX(e.confidence) DESC, e.attractionId ASC
        """,
        countQuery = """
            SELECT COUNT(DISTINCT e.attractionId) FROM SeasonalBloomEstimate e
            WHERE e.baseDate = :baseDate
              AND e.status = :status
              AND e.bloomCategory = :bloomCategory
        """,
    )
    fun findAttractionIdsByBaseDateAndStatusAndBloomCategory(
        @Param("baseDate") baseDate: LocalDate,
        @Param("status") status: BloomStatus,
        @Param("bloomCategory") bloomCategory: BloomCategory,
        pageable: Pageable,
    ): Page<Long>

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
