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
    name = "walking_routes",
    uniqueConstraints = [UniqueConstraint(name = "uk_walking_routes_route_idx", columnNames = ["route_idx"])],
)
class WalkingRoute(
    @Column(name = "route_idx", nullable = false, columnDefinition = "TEXT")
    val routeIdx: String,

    @Column(name = "route_name", columnDefinition = "TEXT")
    var routeName: String? = null,

    @Column(name = "brd_div", columnDefinition = "TEXT")
    var brdDiv: String? = null,

    @Column(name = "theme_nm", columnDefinition = "TEXT")
    var themeNm: String? = null,

    @Column(name = "sigun", columnDefinition = "TEXT")
    var sigun: String? = null,

    @Column(name = "distance", columnDefinition = "TEXT")
    var distance: String? = null,

    @Column(name = "required_time", columnDefinition = "TEXT")
    var requiredTime: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
