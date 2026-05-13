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
    uniqueConstraints = [UniqueConstraint(name = "uk_gallery_photos_content_id", columnNames = ["gal_content_id"])],
)
class GalleryPhoto(
    @Column(name = "gal_content_id", nullable = false, columnDefinition = "TEXT")
    val galContentId: String,

    @Column(name = "gal_content_type_id", columnDefinition = "TEXT")
    var galContentTypeId: String? = null,

    @Column(name = "gal_title", columnDefinition = "TEXT")
    var galTitle: String? = null,

    @Column(name = "gal_web_image_url", columnDefinition = "TEXT")
    var galWebImageUrl: String? = null,

    @Column(name = "gal_created_time", columnDefinition = "TEXT")
    var galCreatedTime: String? = null,

    @Column(name = "gal_modified_time", columnDefinition = "TEXT")
    var galModifiedTime: String? = null,

    @Column(name = "gal_photography_month", columnDefinition = "TEXT")
    var galPhotographyMonth: String? = null,

    @Column(name = "gal_photography_location", columnDefinition = "TEXT")
    var galPhotographyLocation: String? = null,

    @Column(name = "gal_photographer", columnDefinition = "TEXT")
    var galPhotographer: String? = null,

    @Column(name = "gal_search_keyword", columnDefinition = "TEXT")
    var galSearchKeyword: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
