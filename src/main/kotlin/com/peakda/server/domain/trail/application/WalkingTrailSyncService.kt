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
        var saved = 0
        for (item in items) {
            if (item.routeIdx.isBlank()) continue
            val existing = repository.findByRouteIdx(item.routeIdx)
            if (existing == null) repository.save(item.toWalkingRoute()) else existing.applyUpdate(item)
            saved++
        }
        return saved
    }
}

@Service
class WalkingCourseSyncService(
    private val repository: WalkingCourseRepository,
) {
    @Transactional
    fun upsertPage(items: List<CourseItem>): Int {
        var saved = 0
        for (item in items) {
            if (item.crsIdx.isBlank()) continue
            val existing = repository.findByCrsIdx(item.crsIdx)
            if (existing == null) repository.save(item.toWalkingCourse()) else existing.applyUpdate(item)
            saved++
        }
        return saved
    }
}
