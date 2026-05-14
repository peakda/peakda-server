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
    uniqueConstraints = [UniqueConstraint(name = "uk_walking_routes_durunubi_route_id", columnNames = ["durunubi_route_id"])],
)
class WalkingRoute(
    @Column(name = "durunubi_route_id", nullable = false, columnDefinition = "TEXT")
    val durunubiRouteId: String,

    @Column(name = "route_name", columnDefinition = "TEXT")
    var routeName: String? = null,

    @Column(name = "region_division", columnDefinition = "TEXT")
    var regionDivision: String? = null,

    @Column(name = "theme_name", columnDefinition = "TEXT")
    var themeName: String? = null,

    @Column(name = "city_county", columnDefinition = "TEXT")
    var cityCounty: String? = null,

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
