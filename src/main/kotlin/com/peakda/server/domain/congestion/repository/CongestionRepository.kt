package com.peakda.server.domain.congestion.repository

import com.peakda.server.domain.congestion.entity.Congestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val CONGESTION_UPSERT_SQL = """
    INSERT INTO congestions (
        base_date, tourist_attraction_code, tourist_attraction_name, area_code, sigungu_code,
        congestion_rate, created_at, updated_at
    ) VALUES (
        :#{#command.baseDate}, :#{#command.touristAttractionCode}, :#{#command.touristAttractionName},
        :#{#command.areaCode}, :#{#command.sigunguCode}, :#{#command.congestionRate}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_congestions_date_attraction DO UPDATE SET
        tourist_attraction_name = COALESCE(EXCLUDED.tourist_attraction_name, congestions.tourist_attraction_name),
        area_code = COALESCE(EXCLUDED.area_code, congestions.area_code),
        sigungu_code = COALESCE(EXCLUDED.sigungu_code, congestions.sigungu_code),
        congestion_rate = COALESCE(EXCLUDED.congestion_rate, congestions.congestion_rate),
        updated_at = now()
"""

interface CongestionRepository : JpaRepository<Congestion, Long> {
    fun findByBaseDateAndTouristAttractionCode(baseDate: String, touristAttractionCode: String): Congestion?

    @Modifying
    @Query(value = CONGESTION_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: CongestionUpsertCommand): Int
}

data class CongestionUpsertCommand(
    val baseDate: String,
    val touristAttractionCode: String,
    val touristAttractionName: String?,
    val areaCode: String?,
    val sigunguCode: String?,
    val congestionRate: String?,
)
