package com.peakda.server.domain.trail.repository

import com.peakda.server.domain.trail.entity.WalkingCourse
import com.peakda.server.domain.trail.entity.WalkingRoute
import org.springframework.data.jpa.repository.JpaRepository

interface WalkingRouteRepository : JpaRepository<WalkingRoute, Long> {
    fun findByRouteIdx(routeIdx: String): WalkingRoute?
}

interface WalkingCourseRepository : JpaRepository<WalkingCourse, Long> {
    fun findByCrsIdx(crsIdx: String): WalkingCourse?
}
