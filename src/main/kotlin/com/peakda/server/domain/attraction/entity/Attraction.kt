package com.peakda.server.domain.attraction.entity

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
    name = "attractions",
    uniqueConstraints = [UniqueConstraint(name = "uk_attractions_content_id", columnNames = ["content_id"])],
)
class Attraction(
    @Column(name = "content_id", nullable = false, columnDefinition = "TEXT")
    val contentId: String,

    @Column(name = "content_type_id", columnDefinition = "TEXT")
    var contentTypeId: String? = null,

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(name = "addr1", columnDefinition = "TEXT")
    var addr1: String? = null,

    @Column(name = "addr2", columnDefinition = "TEXT")
    var addr2: String? = null,

    @Column(name = "area_code", columnDefinition = "TEXT")
    var areaCode: String? = null,

    @Column(name = "sigungu_code", columnDefinition = "TEXT")
    var sigunguCode: String? = null,

    @Column(name = "map_x")
    var mapX: Double? = null,

    @Column(name = "map_y")
    var mapY: Double? = null,

    @Column(name = "first_image", columnDefinition = "TEXT")
    var firstImage: String? = null,

    @Column(name = "first_image2", columnDefinition = "TEXT")
    var firstImage2: String? = null,

    @Column(name = "cat1", columnDefinition = "TEXT")
    var cat1: String? = null,

    @Column(name = "cat2", columnDefinition = "TEXT")
    var cat2: String? = null,

    @Column(name = "cat3", columnDefinition = "TEXT")
    var cat3: String? = null,

    @Column(name = "created_time", columnDefinition = "TEXT")
    var createdTime: String? = null,

    @Column(name = "modified_time", columnDefinition = "TEXT")
    var modifiedTime: String? = null,

    @Column(name = "visible", nullable = false)
    var visible: Boolean = true,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
