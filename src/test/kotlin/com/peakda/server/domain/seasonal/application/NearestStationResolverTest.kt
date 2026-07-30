package com.peakda.server.domain.seasonal.application

import com.peakda.server.infrastructure.external.kma.asosdaly.AsosStation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NearestStationResolverTest {
    @Test
    fun `여러 지점 중 좌표에 가장 가까운 지점을 고른다`() {
        val stations = listOf(
            station("near", 37.51, 127.01),
            station("far", 35.18, 129.08),
        )

        val nearest = NearestStationResolver.resolve(37.50, 127.00, stations)

        assertThat(nearest?.stationId).isEqualTo("near")
    }

    @Test
    fun `최근접 지점과 함께 미터 단위 거리를 반환한다`() {
        val stations = listOf(station("east", 37.50, 127.01))

        val nearest = NearestStationResolver.resolve(37.50, 127.00, stations)

        assertThat(nearest?.distanceMeters).isBetween(880.0, 890.0)
    }

    @Test
    fun `후보 지점이 비어 있으면 null을 반환한다`() {
        val nearest = NearestStationResolver.resolve(37.50, 127.00, emptyList())

        assertThat(nearest).isNull()
    }

    @Test
    fun `실제 지점 좌표로 남산은 서울을 제주는 제주를 고른다`() {
        val stations = listOf(
            station("108", 37.57142, 126.9658),
            station("184", 33.51411, 126.52969),
        )

        val namsan = NearestStationResolver.resolve(37.55, 126.98, stations)
        val jeju = NearestStationResolver.resolve(33.50, 126.53, stations)

        assertThat(namsan?.stationId).isEqualTo("108")
        assertThat(jeju?.stationId).isEqualTo("184")
    }

    private fun station(stnId: String, latitude: Double, longitude: Double) = AsosStation(
        stnId = stnId,
        name = stnId,
        latitude = latitude,
        longitude = longitude,
        altitude = null,
    )
}
