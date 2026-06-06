package com.peakda.server.domain.seasonal.repository

import com.peakda.server.domain.seasonal.entity.AttractionBloom
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val ATTRACTION_BLOOM_UPSERT_SQL = """
    INSERT INTO attraction_blooms (
        attraction_id, bloom_category, source, confidence, evidence, created_at, updated_at
    ) VALUES (
        :#{#command.attractionId}, :#{#command.bloomCategory}, :#{#command.source},
        :#{#command.confidence}, :#{#command.evidence}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_attraction_blooms_attraction_category_source DO UPDATE SET
        confidence = EXCLUDED.confidence,
        evidence = EXCLUDED.evidence,
        updated_at = now()
"""

interface AttractionBloomRepository : JpaRepository<AttractionBloom, Long> {

    fun findByAttractionId(attractionId: Long): List<AttractionBloom>

    fun findByBloomCategory(bloomCategory: BloomCategory): List<AttractionBloom>

    @Modifying
    @Query(value = ATTRACTION_BLOOM_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: AttractionBloomUpsertCommand): Int
}

/**
 * [AttractionBloom] upsert 입력. enum 은 native 바인딩 모호성을 피하려 `name` 문자열로 전달한다.
 */
data class AttractionBloomUpsertCommand(
    val attractionId: Long,
    val bloomCategory: String,
    val source: String,
    val confidence: Double,
    val evidence: String?,
)
