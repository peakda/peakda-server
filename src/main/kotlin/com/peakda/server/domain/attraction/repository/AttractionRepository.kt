package com.peakda.server.domain.attraction.repository

import com.peakda.server.domain.attraction.entity.Attraction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val ATTRACTION_UPSERT_SQL = """
    INSERT INTO attractions (
        tour_api_content_id, content_type_code, title, address_main, address_detail,
        area_code, sigungu_code, longitude, latitude, primary_image_url, thumbnail_image_url,
        category_major, category_medium, category_minor, external_created_at, external_modified_at,
        visible, created_at, updated_at
    ) VALUES (
        :#{#command.tourApiContentId}, :#{#command.contentTypeCode}, :#{#command.title},
        :#{#command.addressMain}, :#{#command.addressDetail}, :#{#command.areaCode},
        :#{#command.sigunguCode}, :#{#command.longitude}, :#{#command.latitude},
        :#{#command.primaryImageUrl}, :#{#command.thumbnailImageUrl}, :#{#command.categoryMajor},
        :#{#command.categoryMedium}, :#{#command.categoryMinor}, :#{#command.externalCreatedAt},
        :#{#command.externalModifiedAt}, :#{#command.visible}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_attractions_tour_api_content_id DO UPDATE SET
        content_type_code = COALESCE(EXCLUDED.content_type_code, attractions.content_type_code),
        title = EXCLUDED.title,
        address_main = COALESCE(EXCLUDED.address_main, attractions.address_main),
        address_detail = COALESCE(EXCLUDED.address_detail, attractions.address_detail),
        area_code = COALESCE(EXCLUDED.area_code, attractions.area_code),
        sigungu_code = COALESCE(EXCLUDED.sigungu_code, attractions.sigungu_code),
        longitude = COALESCE(EXCLUDED.longitude, attractions.longitude),
        latitude = COALESCE(EXCLUDED.latitude, attractions.latitude),
        primary_image_url = COALESCE(EXCLUDED.primary_image_url, attractions.primary_image_url),
        thumbnail_image_url = COALESCE(EXCLUDED.thumbnail_image_url, attractions.thumbnail_image_url),
        category_major = COALESCE(EXCLUDED.category_major, attractions.category_major),
        category_medium = COALESCE(EXCLUDED.category_medium, attractions.category_medium),
        category_minor = COALESCE(EXCLUDED.category_minor, attractions.category_minor),
        external_created_at = COALESCE(EXCLUDED.external_created_at, attractions.external_created_at),
        external_modified_at = COALESCE(EXCLUDED.external_modified_at, attractions.external_modified_at),
        visible = EXCLUDED.visible,
        updated_at = now()
"""

interface AttractionRepository : JpaRepository<Attraction, Long> {
    fun findByTourApiContentId(tourApiContentId: String): Attraction?

    fun findByVisibleTrue(pageable: Pageable): Page<Attraction>

    @Modifying
    @Query(value = ATTRACTION_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: AttractionUpsertCommand): Int

    @Query(
        """
            SELECT a FROM Attraction a
            WHERE a.visible = true
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
        """,
    )
    fun findVisibleInBoundingBox(
        @Param("minLat") minLat: Double,
        @Param("maxLat") maxLat: Double,
        @Param("minLng") minLng: Double,
        @Param("maxLng") maxLng: Double,
    ): List<Attraction>
}

data class AttractionUpsertCommand(
    val tourApiContentId: String,
    val contentTypeCode: String?,
    val title: String,
    val addressMain: String?,
    val addressDetail: String?,
    val areaCode: String?,
    val sigunguCode: String?,
    val longitude: Double?,
    val latitude: Double?,
    val primaryImageUrl: String?,
    val thumbnailImageUrl: String?,
    val categoryMajor: String?,
    val categoryMedium: String?,
    val categoryMinor: String?,
    val externalCreatedAt: String?,
    val externalModifiedAt: String?,
    val visible: Boolean,
)
