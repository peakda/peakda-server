package com.peakda.server.infrastructure.external.kma

import org.springframework.stereotype.Component
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

@Component
class GridConverter {
    fun toGrid(latitude: Double, longitude: Double): GridCoordinate {
        val ra = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)

        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn

        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = ra * sf / ro.pow(sn)

        var raLat = tan(PI * 0.25 + latitude * DEGRAD * 0.5)
        raLat = ra * sf / raLat.pow(sn)

        var theta = longitude * DEGRAD - olon
        if (theta > PI) theta -= 2.0 * PI
        if (theta < -PI) theta += 2.0 * PI
        theta *= sn

        return GridCoordinate(
            nx = floor(raLat * sin(theta) + XO + 0.5).toInt(),
            ny = floor(ro - raLat * cos(theta) + YO + 0.5).toInt(),
        )
    }

    fun toLatLon(nx: Int, ny: Int): LatLon {
        val ra = RE / GRID
        val slat1 = SLAT1 * DEGRAD
        val slat2 = SLAT2 * DEGRAD
        val olon = OLON * DEGRAD
        val olat = OLAT * DEGRAD

        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = ln(cos(slat1) / cos(slat2)) / ln(sn)

        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) / sn

        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = ra * sf / ro.pow(sn)

        val xn = nx - XO
        val yn = ro - ny + YO
        var raGrid = kotlin.math.sqrt(xn * xn + yn * yn)
        if (sn < 0.0) raGrid = -raGrid

        var alat = (ra * sf / raGrid).pow(1.0 / sn)
        alat = 2.0 * atan2(alat, 1.0) - PI * 0.5

        var theta = if (kotlin.math.abs(xn) <= 0.0) 0.0 else {
            if (kotlin.math.abs(yn) <= 0.0) {
                if (xn < 0.0) -PI * 0.5 else PI * 0.5
            } else {
                atan2(xn, yn)
            }
        }
        theta /= sn

        return LatLon(
            latitude = asin(sin(alat)) * RADDEG,
            longitude = (theta + olon) * RADDEG,
        )
    }

    companion object {
        private const val RE = 6371.00877
        private const val GRID = 5.0
        private const val SLAT1 = 30.0
        private const val SLAT2 = 60.0
        private const val OLON = 126.0
        private const val OLAT = 38.0
        private const val XO = 43.0
        private const val YO = 136.0
        private const val DEGRAD = PI / 180.0
        private const val RADDEG = 180.0 / PI
    }
}

data class GridCoordinate(
    val nx: Int,
    val ny: Int,
)

data class LatLon(
    val latitude: Double,
    val longitude: Double,
)
