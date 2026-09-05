package com.peakda.server.domain.trail.entity

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
    name = "walking_courses",
    uniqueConstraints = [UniqueConstraint(name = "uk_walking_courses_durunubi_course_id", columnNames = ["durunubi_course_id"])],
)
class WalkingCourse(
    @Column(name = "durunubi_course_id", nullable = false, columnDefinition = "TEXT")
    val durunubiCourseId: String,

    @Column(name = "durunubi_route_id", columnDefinition = "TEXT")
    var durunubiRouteId: String? = null,

    @Column(name = "name", columnDefinition = "TEXT")
    var name: String? = null,

    @Column(name = "distance", columnDefinition = "TEXT")
    var distance: String? = null,

    @Column(name = "total_required_time", columnDefinition = "TEXT")
    var totalRequiredTime: String? = null,

    @Column(name = "difficulty_level", columnDefinition = "TEXT")
    var difficultyLevel: String? = null,

    @Column(name = "city_county", columnDefinition = "TEXT")
    var cityCounty: String? = null,

    @Column(name = "region_division", columnDefinition = "TEXT")
    var regionDivision: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
