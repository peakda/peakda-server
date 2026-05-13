package com.peakda.server.domain.trail.application

import com.peakda.server.domain.trail.entity.WalkingCourse
import com.peakda.server.domain.trail.entity.WalkingRoute
import com.peakda.server.infrastructure.external.kto.durunubi.response.CourseItem
import com.peakda.server.infrastructure.external.kto.durunubi.response.RouteItem

fun RouteItem.toWalkingRoute(): WalkingRoute = WalkingRoute(
    routeIdx = routeIdx,
    routeName = routeName.ifBlank { null },
    brdDiv = brdDiv.ifBlank { null },
    themeNm = themeNm.ifBlank { null },
    sigun = sigun.ifBlank { null },
    distance = distance.ifBlank { null },
    requiredTime = requiredTime.ifBlank { null },
)

fun WalkingRoute.applyUpdate(item: RouteItem) {
    routeName = item.routeName.ifBlank { routeName }
    brdDiv = item.brdDiv.ifBlank { brdDiv }
    themeNm = item.themeNm.ifBlank { themeNm }
    sigun = item.sigun.ifBlank { sigun }
    distance = item.distance.ifBlank { distance }
    requiredTime = item.requiredTime.ifBlank { requiredTime }
}

fun CourseItem.toWalkingCourse(): WalkingCourse = WalkingCourse(
    crsIdx = crsIdx,
    routeIdx = routeIdx.ifBlank { null },
    crsKorNm = crsKorNm.ifBlank { null },
    crsDstnc = crsDstnc.ifBlank { null },
    crsTotlRqrmHour = crsTotlRqrmHour.ifBlank { null },
    crsLevel = crsLevel.ifBlank { null },
    sigun = sigun.ifBlank { null },
    brdDiv = brdDiv.ifBlank { null },
)

fun WalkingCourse.applyUpdate(item: CourseItem) {
    routeIdx = item.routeIdx.ifBlank { routeIdx }
    crsKorNm = item.crsKorNm.ifBlank { crsKorNm }
    crsDstnc = item.crsDstnc.ifBlank { crsDstnc }
    crsTotlRqrmHour = item.crsTotlRqrmHour.ifBlank { crsTotlRqrmHour }
    crsLevel = item.crsLevel.ifBlank { crsLevel }
    sigun = item.sigun.ifBlank { sigun }
    brdDiv = item.brdDiv.ifBlank { brdDiv }
}
