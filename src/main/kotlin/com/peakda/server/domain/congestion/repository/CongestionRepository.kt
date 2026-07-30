package com.peakda.server.domain.congestion.repository

import com.peakda.server.domain.congestion.entity.Congestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val CONGESTION_UPSERT_SQL = """
    INSERT INTO congestions (
        base_date, area_code, sigungu_code, tourist_attraction_name,
        congestion_rate, created_at, updated_at
    ) VALUES (
        :#{#command.baseDate}, :#{#command.areaCode}, :#{#command.sigunguCode},
        :#{#command.touristAttractionName}, :#{#command.congestionRate}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_congestions_date_region_attraction DO UPDATE SET
        congestion_rate = COALESCE(EXCLUDED.congestion_rate, congestions.congestion_rate),
        updated_at = now()
"""

interface CongestionRepository : JpaRepository<Congestion, Long> {
    @Modifying
    @Query(value = CONGESTION_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: CongestionUpsertCommand): Int
}

data class CongestionUpsertCommand(
    val baseDate: String,
    val areaCode: String,
    val sigunguCode: String,
    val touristAttractionName: String,
    val congestionRate: String?,
)
