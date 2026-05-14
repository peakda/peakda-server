package com.peakda.server.domain.visitor.repository

import com.peakda.server.domain.visitor.entity.RegionVisitor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val REGION_VISITOR_UPSERT_SQL = """
    INSERT INTO region_visitors (
        base_date, area_code, tourist_type_code, area_name, tourist_type_name,
        visitor_count, created_at, updated_at
    ) VALUES (
        :#{#command.baseDate}, :#{#command.areaCode}, :#{#command.touristTypeCode},
        :#{#command.areaName}, :#{#command.touristTypeName}, :#{#command.visitorCount}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_region_visitors_date_area_type DO UPDATE SET
        area_name = COALESCE(EXCLUDED.area_name, region_visitors.area_name),
        tourist_type_name = COALESCE(EXCLUDED.tourist_type_name, region_visitors.tourist_type_name),
        visitor_count = COALESCE(EXCLUDED.visitor_count, region_visitors.visitor_count),
        updated_at = now()
"""

interface RegionVisitorRepository : JpaRepository<RegionVisitor, Long> {
    fun findByBaseDateAndAreaCodeAndTouristTypeCode(
        baseDate: String,
        areaCode: String,
        touristTypeCode: String,
    ): RegionVisitor?

    @Modifying
    @Query(value = REGION_VISITOR_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: RegionVisitorUpsertCommand): Int
}

data class RegionVisitorUpsertCommand(
    val baseDate: String,
    val areaCode: String,
    val touristTypeCode: String,
    val areaName: String?,
    val touristTypeName: String?,
    val visitorCount: Long?,
)
