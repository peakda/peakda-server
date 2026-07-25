package com.peakda.server.domain.curation.application

data class UpsertCurationRecommendationCommand(
    val title: String,
    val spotId: Long?,
    val placeName: String,
    val latitude: Double?,
    val longitude: Double?,
    val photoUrl: String?,
    val body: String,
)
