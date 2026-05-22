package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.SpotType

data class SpotResolveInput(
    val existingSpotId: Long?,
    val type: SpotType,
    val attractionId: Long?,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val kakaoPlaceId: String?,
    val userId: Long,
)
