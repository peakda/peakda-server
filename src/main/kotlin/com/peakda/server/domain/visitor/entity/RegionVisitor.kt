package com.peakda.server.domain.visitor.entity

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
    name = "region_visitors",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_region_visitors_ymd_area_div",
            columnNames = ["base_ymd", "area_cd", "tou_div_cd"],
        ),
    ],
)
class RegionVisitor(
    @Column(name = "base_ymd", nullable = false, columnDefinition = "TEXT")
    val baseYmd: String,

    @Column(name = "area_cd", nullable = false, columnDefinition = "TEXT")
    val areaCd: String,

    @Column(name = "tou_div_cd", nullable = false, columnDefinition = "TEXT")
    val touDivCd: String,

    @Column(name = "area_nm", columnDefinition = "TEXT")
    var areaNm: String? = null,

    @Column(name = "tou_div_nm", columnDefinition = "TEXT")
    var touDivNm: String? = null,

    @Column(name = "num")
    var num: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
