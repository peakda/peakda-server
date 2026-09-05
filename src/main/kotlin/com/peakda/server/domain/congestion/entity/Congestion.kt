package com.peakda.server.domain.congestion.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 관광지 혼잡도.
 *
 * 집중률 API 응답에는 관광지 코드가 없고 관광지명만 온다. 따라서 자연키는
 * (기준일자, 지역, 시군구, 관광지명) 네 컬럼이다.
 */
@Entity
@Table(
    name = "congestions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_congestions_date_region_attraction",
            columnNames = ["base_date", "area_code", "sigungu_code", "tourist_attraction_name"],
        ),
    ],
)
class Congestion(
    @Column(name = "base_date", nullable = false, columnDefinition = "TEXT")
    val baseDate: String,

    @Column(name = "area_code", nullable = false, columnDefinition = "TEXT")
    val areaCode: String,

    @Column(name = "sigungu_code", nullable = false, columnDefinition = "TEXT")
    val sigunguCode: String,

    @Column(name = "tourist_attraction_name", nullable = false, columnDefinition = "TEXT")
    val touristAttractionName: String,

    @Column(name = "congestion_rate", columnDefinition = "TEXT")
    var congestionRate: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
