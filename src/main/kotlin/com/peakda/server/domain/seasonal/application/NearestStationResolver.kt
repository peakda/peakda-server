package com.peakda.server.domain.seasonal.application

import com.peakda.server.infrastructure.external.kma.asosdaly.AsosStation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object NearestStationResolver {
    /** 좌표에서 가장 가까운 지점과 그 거리(m). 후보가 비면 null. */
    fun resolve(
        latitude: Double,
        longitude: Double,
        stations: List<AsosStation>,
    ): NearestStation? = stations
        .map { station ->
            NearestStation(
                stationId = station.stnId,
                distanceMeters = haversine(latitude, longitude, station.latitude, station.longitude),
            )
        }
        .minByOrNull { it.distanceMeters }

    private fun haversine(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double,
    ): Double {
        val latitudeDelta = Math.toRadians(latitude2 - latitude1)
        val longitudeDelta = Math.toRadians(longitude2 - longitude1)
        val latitude1Radians = Math.toRadians(latitude1)
        val latitude2Radians = Math.toRadians(latitude2)
        val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(latitude1Radians) * cos(latitude2Radians) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}

data class NearestStation(
    val stationId: String,
    val distanceMeters: Double,
)
