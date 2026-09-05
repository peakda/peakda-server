package com.peakda.server.domain.gallery.repository

import com.peakda.server.domain.gallery.entity.GalleryPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val GALLERY_PHOTO_UPSERT_SQL = """
    INSERT INTO gallery_photos (
        tour_api_content_id, content_type_code, title, web_image_url, external_created_at,
        external_modified_at, photography_month, photography_location, photographer, search_keyword,
        created_at, updated_at
    ) VALUES (
        :#{#command.tourApiContentId}, :#{#command.contentTypeCode}, :#{#command.title},
        :#{#command.webImageUrl}, :#{#command.externalCreatedAt}, :#{#command.externalModifiedAt},
        :#{#command.photographyMonth}, :#{#command.photographyLocation}, :#{#command.photographer},
        :#{#command.searchKeyword}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_gallery_photos_tour_api_content_id DO UPDATE SET
        content_type_code = COALESCE(EXCLUDED.content_type_code, gallery_photos.content_type_code),
        title = COALESCE(EXCLUDED.title, gallery_photos.title),
        web_image_url = COALESCE(EXCLUDED.web_image_url, gallery_photos.web_image_url),
        external_created_at = COALESCE(EXCLUDED.external_created_at, gallery_photos.external_created_at),
        external_modified_at = COALESCE(EXCLUDED.external_modified_at, gallery_photos.external_modified_at),
        photography_month = COALESCE(EXCLUDED.photography_month, gallery_photos.photography_month),
        photography_location = COALESCE(EXCLUDED.photography_location, gallery_photos.photography_location),
        photographer = COALESCE(EXCLUDED.photographer, gallery_photos.photographer),
        search_keyword = COALESCE(EXCLUDED.search_keyword, gallery_photos.search_keyword),
        updated_at = now()
"""

interface GalleryPhotoRepository : JpaRepository<GalleryPhoto, Long> {
    fun findByTourApiContentId(tourApiContentId: String): GalleryPhoto?

    @Modifying
    @Query(value = GALLERY_PHOTO_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: GalleryPhotoUpsertCommand): Int
}

data class GalleryPhotoUpsertCommand(
    val tourApiContentId: String,
    val contentTypeCode: String?,
    val title: String?,
    val webImageUrl: String?,
    val externalCreatedAt: String?,
    val externalModifiedAt: String?,
    val photographyMonth: String?,
    val photographyLocation: String?,
    val photographer: String?,
    val searchKeyword: String?,
)
