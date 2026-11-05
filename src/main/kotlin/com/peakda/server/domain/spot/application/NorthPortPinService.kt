package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.presentation.response.PromotionalMapPinResponse
import org.springframework.stereotype.Service

@Service
class NorthPortPinService(
    private val properties: NorthPortPinProperties,
) {
    fun findInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
    ): List<PromotionalMapPinResponse> {
        if (!properties.enabled) return emptyList()
        val latitude = requireNotNull(properties.latitude)
        val longitude = requireNotNull(properties.longitude)
        if (latitude !in minLat..maxLat || longitude !in minLng..maxLng) return emptyList()

        return listOf(
            PromotionalMapPinResponse(
                id = "BUSAN_NORTH_PORT",
                name = "부산 북항",
                latitude = latitude,
                longitude = longitude,
                iconKey = "SHARK",
                linkUrl = properties.linkUrl,
            ),
        )
    }
}
