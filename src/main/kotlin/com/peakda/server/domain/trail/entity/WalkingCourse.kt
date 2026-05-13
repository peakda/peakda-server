package com.peakda.server.domain.trail.entity

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
    name = "walking_courses",
    uniqueConstraints = [UniqueConstraint(name = "uk_walking_courses_crs_idx", columnNames = ["crs_idx"])],
)
class WalkingCourse(
    @Column(name = "crs_idx", nullable = false, columnDefinition = "TEXT")
    val crsIdx: String,

    @Column(name = "route_idx", columnDefinition = "TEXT")
    var routeIdx: String? = null,

    @Column(name = "crs_kor_nm", columnDefinition = "TEXT")
    var crsKorNm: String? = null,

    @Column(name = "crs_dstnc", columnDefinition = "TEXT")
    var crsDstnc: String? = null,

    @Column(name = "crs_totl_rqrm_hour", columnDefinition = "TEXT")
    var crsTotlRqrmHour: String? = null,

    @Column(name = "crs_level", columnDefinition = "TEXT")
    var crsLevel: String? = null,

    @Column(name = "sigun", columnDefinition = "TEXT")
    var sigun: String? = null,

    @Column(name = "brd_div", columnDefinition = "TEXT")
    var brdDiv: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
