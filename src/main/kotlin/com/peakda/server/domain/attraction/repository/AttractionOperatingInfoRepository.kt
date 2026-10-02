package com.peakda.server.domain.attraction.repository

import com.peakda.server.domain.attraction.entity.AttractionOperatingInfo
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val ATTRACTION_OPERATING_INFO_UPSERT_SQL = """
    INSERT INTO attraction_operating_infos (
        attraction_id, operating_hours, closed_days, admission_fee, parking, source_modified_at, created_at, updated_at
    ) VALUES (
        :#{#command.attractionId}, :#{#command.operatingHours}, :#{#command.closedDays},
        :#{#command.admissionFee}, :#{#command.parking}, :#{#command.sourceModifiedAt}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_attraction_operating_infos_attraction DO UPDATE SET
        operating_hours = EXCLUDED.operating_hours,
        closed_days = EXCLUDED.closed_days,
        admission_fee = EXCLUDED.admission_fee,
        parking = EXCLUDED.parking,
        source_modified_at = EXCLUDED.source_modified_at,
        updated_at = now()
"""

interface AttractionOperatingInfoRepository : JpaRepository<AttractionOperatingInfo, Long> {
    fun findByAttractionId(attractionId: Long): AttractionOperatingInfo?

    /** 운영 정보 행이 아직 없는 공개 명소. id 순. */
    @Query(
        """
            SELECT new com.peakda.server.domain.attraction.repository.AttractionOperatingInfoTarget(
                a.id, a.tourApiContentId, a.contentTypeCode, a.externalModifiedAt
            )
            FROM Attraction a
            WHERE a.visible = true
              AND a.contentTypeCode IN :contentTypeCodes
              AND NOT EXISTS (SELECT 1 FROM AttractionOperatingInfo o WHERE o.attractionId = a.id)
            ORDER BY a.id
        """,
    )
    fun findTargetsWithoutOperatingInfo(
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
        pageable: Pageable,
    ): List<AttractionOperatingInfoTarget>

    /** 운영 정보를 받은 뒤 관광공사 수정 시각이 바뀐 공개 명소. id 순. */
    @Query(
        """
            SELECT new com.peakda.server.domain.attraction.repository.AttractionOperatingInfoTarget(
                a.id, a.tourApiContentId, a.contentTypeCode, a.externalModifiedAt
            )
            FROM Attraction a
            JOIN AttractionOperatingInfo o ON o.attractionId = a.id
            WHERE a.visible = true
              AND a.contentTypeCode IN :contentTypeCodes
              AND a.externalModifiedAt IS NOT NULL
              AND (o.sourceModifiedAt IS NULL OR o.sourceModifiedAt <> a.externalModifiedAt)
            ORDER BY a.id
        """,
    )
    fun findTargetsWithOutdatedOperatingInfo(
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
        pageable: Pageable,
    ): List<AttractionOperatingInfoTarget>

    @Modifying
    @Query(value = ATTRACTION_OPERATING_INFO_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: AttractionOperatingInfoUpsertCommand): Int
}

data class AttractionOperatingInfoUpsertCommand(
    val attractionId: Long,
    val operatingHours: String?,
    val closedDays: String?,
    val admissionFee: String?,
    val parking: String?,
    val sourceModifiedAt: String?,
)
