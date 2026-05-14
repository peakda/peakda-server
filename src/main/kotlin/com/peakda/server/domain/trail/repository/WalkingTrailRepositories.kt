package com.peakda.server.domain.trail.repository

import com.peakda.server.domain.trail.entity.WalkingCourse
import com.peakda.server.domain.trail.entity.WalkingRoute
import org.springframework.data.jpa.repository.JpaRepository

interface WalkingRouteRepository : JpaRepository<WalkingRoute, Long> {
    fun findByDurunubiRouteId(durunubiRouteId: String): WalkingRoute?
}

interface WalkingCourseRepository : JpaRepository<WalkingCourse, Long> {
    fun findByDurunubiCourseId(durunubiCourseId: String): WalkingCourse?
}
