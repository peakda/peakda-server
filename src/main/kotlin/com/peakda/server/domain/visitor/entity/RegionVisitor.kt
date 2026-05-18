package com.peakda.server.domain.visitor.entity

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
    name = "region_visitors",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_region_visitors_date_area_type",
            columnNames = ["base_date", "area_code", "tourist_type_code"],
        ),
    ],
)
class RegionVisitor(
    @Column(name = "base_date", nullable = false, columnDefinition = "TEXT")
    val baseDate: String,

    @Column(name = "area_code", nullable = false, columnDefinition = "TEXT")
    val areaCode: String,

    @Column(name = "tourist_type_code", nullable = false, columnDefinition = "TEXT")
    val touristTypeCode: String,

    @Column(name = "area_name", columnDefinition = "TEXT")
    var areaName: String? = null,

    @Column(name = "tourist_type_name", columnDefinition = "TEXT")
    var touristTypeName: String? = null,

    @Column(name = "visitor_count")
    var visitorCount: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
