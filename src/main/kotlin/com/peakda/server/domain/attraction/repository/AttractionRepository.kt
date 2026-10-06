package com.peakda.server.domain.attraction.repository

import com.peakda.server.domain.attraction.entity.Attraction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

private const val ATTRACTION_UPSERT_SQL = """
    INSERT INTO attractions (
        tour_api_content_id, content_type_code, title, address_main, address_detail,
        area_code, sigungu_code, legal_dong_area_code, legal_dong_sigungu_code,
        longitude, latitude, primary_image_url, thumbnail_image_url,
        category_major, category_medium, category_minor, lcls_systm_major, lcls_systm_medium, lcls_systm_minor,
        external_created_at, external_modified_at, visible, created_at, updated_at
    ) VALUES (
        :#{#command.tourApiContentId}, :#{#command.contentTypeCode}, :#{#command.title},
        :#{#command.addressMain}, :#{#command.addressDetail}, :#{#command.areaCode},
        :#{#command.sigunguCode}, :#{#command.legalDongAreaCode}, :#{#command.legalDongSigunguCode},
        :#{#command.longitude}, :#{#command.latitude},
        :#{#command.primaryImageUrl}, :#{#command.thumbnailImageUrl}, :#{#command.categoryMajor},
        :#{#command.categoryMedium}, :#{#command.categoryMinor}, :#{#command.lclsSystmMajor},
        :#{#command.lclsSystmMedium}, :#{#command.lclsSystmMinor}, :#{#command.externalCreatedAt},
        :#{#command.externalModifiedAt}, :#{#command.visible}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_attractions_tour_api_content_id DO UPDATE SET
        content_type_code = COALESCE(EXCLUDED.content_type_code, attractions.content_type_code),
        title = EXCLUDED.title,
        address_main = COALESCE(EXCLUDED.address_main, attractions.address_main),
        address_detail = COALESCE(EXCLUDED.address_detail, attractions.address_detail),
        area_code = COALESCE(EXCLUDED.area_code, attractions.area_code),
        sigungu_code = COALESCE(EXCLUDED.sigungu_code, attractions.sigungu_code),
        legal_dong_area_code = COALESCE(EXCLUDED.legal_dong_area_code, attractions.legal_dong_area_code),
        legal_dong_sigungu_code = COALESCE(EXCLUDED.legal_dong_sigungu_code, attractions.legal_dong_sigungu_code),
        longitude = COALESCE(EXCLUDED.longitude, attractions.longitude),
        latitude = COALESCE(EXCLUDED.latitude, attractions.latitude),
        primary_image_url = COALESCE(EXCLUDED.primary_image_url, attractions.primary_image_url),
        thumbnail_image_url = COALESCE(EXCLUDED.thumbnail_image_url, attractions.thumbnail_image_url),
        category_major = COALESCE(EXCLUDED.category_major, attractions.category_major),
        category_medium = COALESCE(EXCLUDED.category_medium, attractions.category_medium),
        category_minor = COALESCE(EXCLUDED.category_minor, attractions.category_minor),
        lcls_systm_major = COALESCE(EXCLUDED.lcls_systm_major, attractions.lcls_systm_major),
        lcls_systm_medium = COALESCE(EXCLUDED.lcls_systm_medium, attractions.lcls_systm_medium),
        lcls_systm_minor = COALESCE(EXCLUDED.lcls_systm_minor, attractions.lcls_systm_minor),
        external_created_at = COALESCE(EXCLUDED.external_created_at, attractions.external_created_at),
        external_modified_at = COALESCE(EXCLUDED.external_modified_at, attractions.external_modified_at),
        visible = EXCLUDED.visible,
        updated_at = now()
"""

interface AttractionRepository : JpaRepository<Attraction, Long> {
    fun findByTourApiContentId(tourApiContentId: String): Attraction?

    fun findByVisibleTrue(pageable: Pageable): Page<Attraction>

    fun findByVisibleTrueAndContentTypeCodeIn(
        contentTypeCodes: Collection<String>,
        pageable: Pageable,
    ): Slice<Attraction>

    @Query(
        """
            SELECT a.id FROM Attraction a
            WHERE a.id IN :ids
              AND a.visible = true
              AND a.contentTypeCode IN :contentTypeCodes
        """,
    )
    fun findVisibleIdsByIdInAndContentTypes(
        @Param("ids") ids: Collection<Long>,
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
    ): List<Long>

    @Query(
        """
            SELECT new com.peakda.server.domain.attraction.repository.AttractionNameCandidate(a.id, a.title)
            FROM Attraction a
            WHERE a.visible = true
              AND a.legalDongSigunguCode = :legalDongSigunguCode
              AND a.contentTypeCode IN :contentTypeCodes
        """,
    )
    fun findNameCandidatesBySigungu(
        @Param("legalDongSigunguCode") legalDongSigunguCode: String,
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
    ): List<AttractionNameCandidate>

    @Query(
        """
            SELECT new com.peakda.server.domain.attraction.repository.AttractionNameCandidate(a.id, a.title)
            FROM Attraction a
            WHERE a.visible = true
              AND a.legalDongAreaCode IN :legalDongAreaCodes
              AND a.contentTypeCode IN :contentTypeCodes
        """,
    )
    fun findNameCandidatesByAreas(
        @Param("legalDongAreaCodes") legalDongAreaCodes: Collection<String>,
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
    ): List<AttractionNameCandidate>

    @Modifying
    @Query(value = ATTRACTION_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: AttractionUpsertCommand): Int

    @Query(
        """
            SELECT a FROM Attraction a
            WHERE a.visible = true
              AND a.contentTypeCode IN :contentTypeCodes
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
        """,
    )
    fun findVisibleInBoundingBoxByContentTypes(
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
        @Param("minLat") minLat: Double,
        @Param("maxLat") maxLat: Double,
        @Param("minLng") minLng: Double,
        @Param("maxLng") maxLng: Double,
    ): List<Attraction>

    /** 현재 썸네일 URL 을 아직 확인하지 않은 공개 명소. id 순. */
    @Query(
        """
            SELECT new com.peakda.server.domain.attraction.repository.AttractionThumbnailCheckTarget(a.id, a.thumbnailImageUrl)
            FROM Attraction a
            WHERE a.visible = true
              AND a.contentTypeCode IN :contentTypeCodes
              AND a.thumbnailImageUrl IS NOT NULL
              AND (a.thumbnailCheckedUrl IS NULL OR a.thumbnailCheckedUrl <> a.thumbnailImageUrl)
            ORDER BY a.id
        """,
    )
    fun findThumbnailUncheckedTargets(
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
        pageable: Pageable,
    ): List<AttractionThumbnailCheckTarget>

    /** 현재 썸네일 URL 을 [checkedBefore] 이전에 확인한 공개 명소. 오래 전에 확인한 순. */
    @Query(
        """
            SELECT new com.peakda.server.domain.attraction.repository.AttractionThumbnailCheckTarget(a.id, a.thumbnailImageUrl)
            FROM Attraction a
            WHERE a.visible = true
              AND a.contentTypeCode IN :contentTypeCodes
              AND a.thumbnailImageUrl IS NOT NULL
              AND a.thumbnailCheckedUrl = a.thumbnailImageUrl
              AND a.thumbnailCheckedAt < :checkedBefore
            ORDER BY a.thumbnailCheckedAt, a.id
        """,
    )
    fun findThumbnailCheckedBefore(
        @Param("contentTypeCodes") contentTypeCodes: Collection<String>,
        @Param("checkedBefore") checkedBefore: Instant,
        pageable: Pageable,
    ): List<AttractionThumbnailCheckTarget>

    @Modifying
    @Query(
        """
            UPDATE Attraction a
            SET a.thumbnailCheckedUrl = :url, a.thumbnailMissing = :missing, a.thumbnailCheckedAt = :checkedAt
            WHERE a.id = :id
        """,
    )
    fun updateThumbnailCheck(
        @Param("id") id: Long,
        @Param("url") url: String,
        @Param("missing") missing: Boolean,
        @Param("checkedAt") checkedAt: Instant,
    ): Int
}

data class AttractionUpsertCommand(
    val tourApiContentId: String,
    val contentTypeCode: String?,
    val title: String,
    val addressMain: String?,
    val addressDetail: String?,
    val areaCode: String?,
    val sigunguCode: String?,
    val legalDongAreaCode: String?,
    val legalDongSigunguCode: String?,
    val longitude: Double?,
    val latitude: Double?,
    val primaryImageUrl: String?,
    val thumbnailImageUrl: String?,
    val categoryMajor: String?,
    val categoryMedium: String?,
    val categoryMinor: String?,
    val lclsSystmMajor: String?,
    val lclsSystmMedium: String?,
    val lclsSystmMinor: String?,
    val externalCreatedAt: String?,
    val externalModifiedAt: String?,
    val visible: Boolean,
)
