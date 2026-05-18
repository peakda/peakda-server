package com.peakda.server.domain.attraction.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "attractions",
    uniqueConstraints = [UniqueConstraint(name = "uk_attractions_tour_api_content_id", columnNames = ["tour_api_content_id"])],
)
class Attraction(
    @Column(name = "tour_api_content_id", nullable = false, columnDefinition = "TEXT")
    val tourApiContentId: String,

    @Column(name = "content_type_code", columnDefinition = "TEXT")
    var contentTypeCode: String? = null,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(name = "address_main", columnDefinition = "TEXT")
    var addressMain: String? = null,

    @Column(name = "address_detail", columnDefinition = "TEXT")
    var addressDetail: String? = null,

    @Column(name = "area_code", columnDefinition = "TEXT")
    var areaCode: String? = null,

    @Column(name = "sigungu_code", columnDefinition = "TEXT")
    var sigunguCode: String? = null,

    @Column(name = "longitude")
    var longitude: Double? = null,

    @Column(name = "latitude")
    var latitude: Double? = null,

    @Column(name = "primary_image_url", columnDefinition = "TEXT")
    var primaryImageUrl: String? = null,

    @Column(name = "thumbnail_image_url", columnDefinition = "TEXT")
    var thumbnailImageUrl: String? = null,

    @Column(name = "category_major", columnDefinition = "TEXT")
    var categoryMajor: String? = null,

    @Column(name = "category_medium", columnDefinition = "TEXT")
    var categoryMedium: String? = null,

    @Column(name = "category_minor", columnDefinition = "TEXT")
    var categoryMinor: String? = null,

    @Column(name = "external_created_at", columnDefinition = "TEXT")
    var externalCreatedAt: String? = null,

    @Column(name = "external_modified_at", columnDefinition = "TEXT")
    var externalModifiedAt: String? = null,

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
