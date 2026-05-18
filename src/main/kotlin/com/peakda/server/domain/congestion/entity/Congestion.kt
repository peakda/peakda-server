package com.peakda.server.domain.congestion.entity

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
    name = "congestions",
    uniqueConstraints = [UniqueConstraint(name = "uk_congestions_date_attraction", columnNames = ["base_date", "tourist_attraction_code"])],
)
class Congestion(
    @Column(name = "base_date", nullable = false, columnDefinition = "TEXT")
    val baseDate: String,

    @Column(name = "tourist_attraction_code", nullable = false, columnDefinition = "TEXT")
    val touristAttractionCode: String,

    @Column(name = "tourist_attraction_name", columnDefinition = "TEXT")
    var touristAttractionName: String? = null,

    @Column(name = "area_code", columnDefinition = "TEXT")
    var areaCode: String? = null,

    @Column(name = "sigungu_code", columnDefinition = "TEXT")
    var sigunguCode: String? = null,

    @Column(name = "congestion_rate", columnDefinition = "TEXT")
    var congestionRate: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
