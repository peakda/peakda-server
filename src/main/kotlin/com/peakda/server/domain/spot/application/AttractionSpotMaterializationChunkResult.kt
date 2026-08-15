package com.peakda.server.domain.spot.application

data class AttractionSpotMaterializationChunkResult(
    val processed: Int,
    val skippedNoCoordinates: Int,
)
