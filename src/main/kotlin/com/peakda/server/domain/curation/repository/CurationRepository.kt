package com.peakda.server.domain.curation.repository

import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface CurationRepository : JpaRepository<Curation, Long> {

    /** 발행된 큐레이션을 최신 주차순으로 (탐색 카드·목록). */
    fun findByStatusOrderByWeekStartDateDesc(status: CurationStatus, pageable: Pageable): Page<Curation>

    fun findAllByOrderByWeekStartDateDesc(pageable: Pageable): Page<Curation>

    fun findByIdAndStatus(id: Long, status: CurationStatus): Curation?

    fun findCurationById(id: Long): Curation?

    fun findByWeekStartDate(weekStartDate: LocalDate): Curation?

    @Query(
        nativeQuery = true,
        value = """
            SELECT counts.curation_id AS "curationId",
                   CAST(SUM(counts.chapter_count) AS BIGINT) AS "chapterCount",
                   CAST(SUM(counts.recommendation_count) AS BIGINT) AS "recommendationCount"
            FROM (
                SELECT c.curation_id AS curation_id,
                       COUNT(*) AS chapter_count,
                       0::BIGINT AS recommendation_count
                FROM curation_chapters c
                WHERE c.curation_id IN (:curationIds)
                GROUP BY c.curation_id

                UNION ALL

                SELECT r.curation_id AS curation_id,
                       0::BIGINT AS chapter_count,
                       COUNT(*) AS recommendation_count
                FROM curation_recommendations r
                WHERE r.curation_id IN (:curationIds)
                GROUP BY r.curation_id
            ) counts
            GROUP BY counts.curation_id
        """,
    )
    fun countChildrenByCurationIdIn(
        @Param("curationIds") curationIds: Collection<Long>,
    ): List<CurationChildCounts>
}

interface CurationChildCounts {
    val curationId: Long
    val chapterCount: Long
    val recommendationCount: Long
}
