package com.peakda.server.domain.spot.application

data class AttractionSpotMaterializationResult(
    val processed: Int,
    val skippedNoCoordinates: Int,
    val pages: Int,
)
