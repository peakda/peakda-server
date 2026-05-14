package com.peakda.server.domain.trail.application

import com.peakda.server.domain.trail.entity.WalkingCourse
import com.peakda.server.domain.trail.entity.WalkingRoute
import com.peakda.server.infrastructure.external.kto.durunubi.response.CourseItem
import com.peakda.server.infrastructure.external.kto.durunubi.response.RouteItem

fun RouteItem.toWalkingRoute(): WalkingRoute = WalkingRoute(
    durunubiRouteId = routeIdx,
    routeName = routeName.ifBlank { null },
    regionDivision = brdDiv.ifBlank { null },
    themeName = themeNm.ifBlank { null },
    cityCounty = sigun.ifBlank { null },
    distance = distance.ifBlank { null },
    requiredTime = requiredTime.ifBlank { null },
)

fun WalkingRoute.applyUpdate(item: RouteItem) {
    routeName = item.routeName.ifBlank { routeName }
    regionDivision = item.brdDiv.ifBlank { regionDivision }
    themeName = item.themeNm.ifBlank { themeName }
    cityCounty = item.sigun.ifBlank { cityCounty }
    distance = item.distance.ifBlank { distance }
    requiredTime = item.requiredTime.ifBlank { requiredTime }
}

fun CourseItem.toWalkingCourse(): WalkingCourse = WalkingCourse(
    durunubiCourseId = crsIdx,
    durunubiRouteId = routeIdx.ifBlank { null },
    name = crsKorNm.ifBlank { null },
    distance = crsDstnc.ifBlank { null },
    totalRequiredTime = crsTotlRqrmHour.ifBlank { null },
    difficultyLevel = crsLevel.ifBlank { null },
    cityCounty = sigun.ifBlank { null },
    regionDivision = brdDiv.ifBlank { null },
)

fun WalkingCourse.applyUpdate(item: CourseItem) {
    durunubiRouteId = item.routeIdx.ifBlank { durunubiRouteId }
    name = item.crsKorNm.ifBlank { name }
    distance = item.crsDstnc.ifBlank { distance }
    totalRequiredTime = item.crsTotlRqrmHour.ifBlank { totalRequiredTime }
    difficultyLevel = item.crsLevel.ifBlank { difficultyLevel }
    cityCounty = item.sigun.ifBlank { cityCounty }
    regionDivision = item.brdDiv.ifBlank { regionDivision }
}
