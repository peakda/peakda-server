package com.peakda.server.domain.trail.repository

import com.peakda.server.domain.trail.entity.WalkingCourse
import com.peakda.server.domain.trail.entity.WalkingRoute
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val WALKING_ROUTE_UPSERT_SQL = """
    INSERT INTO walking_routes (
        durunubi_route_id, route_name, region_division, theme_name, city_county,
        distance, required_time, created_at, updated_at
    ) VALUES (
        :#{#command.durunubiRouteId}, :#{#command.routeName}, :#{#command.regionDivision},
        :#{#command.themeName}, :#{#command.cityCounty}, :#{#command.distance},
        :#{#command.requiredTime}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_walking_routes_durunubi_route_id DO UPDATE SET
        route_name = COALESCE(EXCLUDED.route_name, walking_routes.route_name),
        region_division = COALESCE(EXCLUDED.region_division, walking_routes.region_division),
        theme_name = COALESCE(EXCLUDED.theme_name, walking_routes.theme_name),
        city_county = COALESCE(EXCLUDED.city_county, walking_routes.city_county),
        distance = COALESCE(EXCLUDED.distance, walking_routes.distance),
        required_time = COALESCE(EXCLUDED.required_time, walking_routes.required_time),
        updated_at = now()
"""

private const val WALKING_COURSE_UPSERT_SQL = """
    INSERT INTO walking_courses (
        durunubi_course_id, durunubi_route_id, name, distance, total_required_time,
        difficulty_level, city_county, region_division, created_at, updated_at
    ) VALUES (
        :#{#command.durunubiCourseId}, :#{#command.durunubiRouteId}, :#{#command.name},
        :#{#command.distance}, :#{#command.totalRequiredTime}, :#{#command.difficultyLevel},
        :#{#command.cityCounty}, :#{#command.regionDivision}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_walking_courses_durunubi_course_id DO UPDATE SET
        durunubi_route_id = COALESCE(EXCLUDED.durunubi_route_id, walking_courses.durunubi_route_id),
        name = COALESCE(EXCLUDED.name, walking_courses.name),
        distance = COALESCE(EXCLUDED.distance, walking_courses.distance),
        total_required_time = COALESCE(EXCLUDED.total_required_time, walking_courses.total_required_time),
        difficulty_level = COALESCE(EXCLUDED.difficulty_level, walking_courses.difficulty_level),
        city_county = COALESCE(EXCLUDED.city_county, walking_courses.city_county),
        region_division = COALESCE(EXCLUDED.region_division, walking_courses.region_division),
        updated_at = now()
"""

interface WalkingRouteRepository : JpaRepository<WalkingRoute, Long> {
    fun findByDurunubiRouteId(durunubiRouteId: String): WalkingRoute?

    @Modifying
    @Query(value = WALKING_ROUTE_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: WalkingRouteUpsertCommand): Int
}

interface WalkingCourseRepository : JpaRepository<WalkingCourse, Long> {
    fun findByDurunubiCourseId(durunubiCourseId: String): WalkingCourse?

    @Modifying
    @Query(value = WALKING_COURSE_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: WalkingCourseUpsertCommand): Int
}

data class WalkingRouteUpsertCommand(
    val durunubiRouteId: String,
    val routeName: String?,
    val regionDivision: String?,
    val themeName: String?,
    val cityCounty: String?,
    val distance: String?,
    val requiredTime: String?,
)

data class WalkingCourseUpsertCommand(
    val durunubiCourseId: String,
    val durunubiRouteId: String?,
    val name: String?,
    val distance: String?,
    val totalRequiredTime: String?,
    val difficultyLevel: String?,
    val cityCounty: String?,
    val regionDivision: String?,
)
