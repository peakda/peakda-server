package com.peakda.server.domain.weather.repository

import com.peakda.server.domain.weather.entity.AttractionForecastArea
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

private const val ATTRACTION_FORECAST_AREA_UPSERT_SQL = """
    INSERT INTO attraction_forecast_areas (
        attraction_id, grid_x, grid_y, mid_region_code, created_at, updated_at
    ) VALUES (
        :#{#command.attractionId}, :#{#command.gridX}, :#{#command.gridY}, :#{#command.midRegionCode}, now(), now()
    )
    ON CONFLICT ON CONSTRAINT uk_attraction_forecast_areas_attraction DO UPDATE SET
        grid_x = EXCLUDED.grid_x,
        grid_y = EXCLUDED.grid_y,
        mid_region_code = EXCLUDED.mid_region_code,
        updated_at = now()
"""

interface AttractionForecastAreaRepository : JpaRepository<AttractionForecastArea, Long> {
    fun findByAttractionId(attractionId: Long): AttractionForecastArea?

    /** 명소가 많이 모인 격자부터. 단기예보 수집 격자 수에 상한을 둘 때 우선순위로 쓴다. */
    @Query(
        """
            SELECT new com.peakda.server.domain.weather.repository.ForecastGrid(a.gridX, a.gridY, COUNT(a))
            FROM AttractionForecastArea a
            GROUP BY a.gridX, a.gridY
            ORDER BY COUNT(a) DESC, a.gridX ASC, a.gridY ASC
        """,
    )
    fun findGridsByAttractionCount(pageable: Pageable): List<ForecastGrid>

    @Modifying
    @Query(value = ATTRACTION_FORECAST_AREA_UPSERT_SQL, nativeQuery = true)
    fun upsert(@Param("command") command: AttractionForecastAreaUpsertCommand): Int
}

data class AttractionForecastAreaUpsertCommand(
    val attractionId: Long,
    val gridX: Int,
    val gridY: Int,
    val midRegionCode: String?,
)
