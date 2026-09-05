package com.peakda.server.domain.trail.application

import com.peakda.server.domain.trail.repository.WalkingCourseRepository
import com.peakda.server.domain.trail.repository.WalkingRouteRepository
import com.peakda.server.infrastructure.external.kto.durunubi.response.CourseItem
import com.peakda.server.infrastructure.external.kto.durunubi.response.RouteItem
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WalkingRouteSyncService(
    private val repository: WalkingRouteRepository,
) {
    @Transactional
    fun upsertPage(items: List<RouteItem>): Int {
        return items
            .filter { it.routeIdx.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}

@Service
class WalkingCourseSyncService(
    private val repository: WalkingCourseRepository,
) {
    @Transactional
    fun upsertPage(items: List<CourseItem>): Int {
        return items
            .filter { it.crsIdx.isNotBlank() }
            .sumOf { repository.upsert(it.toUpsertCommand()) }
    }
}
