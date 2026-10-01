package com.peakda.server.domain.weather.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 명소가 속한 기상청 예보 구역.
 *
 * - 단기예보: 명소 좌표를 변환한 5km 격자(nx, ny)
 * - 중기예보: 법정동 시도·시군구로 정한 예보 구역(`weather_mid_forecasts.region_code` 와 같은 값)
 */
@Entity
@Table(
    name = "attraction_forecast_areas",
    uniqueConstraints = [UniqueConstraint(name = "uk_attraction_forecast_areas_attraction", columnNames = ["attraction_id"])],
)
class AttractionForecastArea(
    @Column(name = "attraction_id", nullable = false)
    val attractionId: Long,

    @Column(name = "grid_x", nullable = false)
    var gridX: Int,

    @Column(name = "grid_y", nullable = false)
    var gridY: Int,

    @Column(name = "mid_region_code", columnDefinition = "TEXT")
    var midRegionCode: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
