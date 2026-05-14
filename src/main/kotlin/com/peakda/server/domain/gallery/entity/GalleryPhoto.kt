package com.peakda.server.domain.gallery.entity

import com.peakda.server.global.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "gallery_photos",
    uniqueConstraints = [UniqueConstraint(name = "uk_gallery_photos_tour_api_content_id", columnNames = ["tour_api_content_id"])],
)
class GalleryPhoto(
    @Column(name = "tour_api_content_id", nullable = false, columnDefinition = "TEXT")
    val tourApiContentId: String,

    @Column(name = "content_type_code", columnDefinition = "TEXT")
    var contentTypeCode: String? = null,

    @Column(name = "title", columnDefinition = "TEXT")
    var title: String? = null,

    @Column(name = "web_image_url", columnDefinition = "TEXT")
    var webImageUrl: String? = null,

    @Column(name = "external_created_at", columnDefinition = "TEXT")
    var externalCreatedAt: String? = null,

    @Column(name = "external_modified_at", columnDefinition = "TEXT")
    var externalModifiedAt: String? = null,

    @Column(name = "photography_month", columnDefinition = "TEXT")
    var photographyMonth: String? = null,

    @Column(name = "photography_location", columnDefinition = "TEXT")
    var photographyLocation: String? = null,

    @Column(name = "photographer", columnDefinition = "TEXT")
    var photographer: String? = null,

    @Column(name = "search_keyword", columnDefinition = "TEXT")
    var searchKeyword: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
