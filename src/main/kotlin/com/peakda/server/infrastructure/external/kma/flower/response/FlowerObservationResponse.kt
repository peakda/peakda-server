package com.peakda.server.infrastructure.external.kma.flower.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowerObservationResponse(
    val places: List<FlowerPlace> = emptyList(),
    val flower: FlowerDetail? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowerPlace(
    val obsPlace: String = "",
    val status: String = "",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FlowerDetail(
    val treeType: String? = null,
    val obsPlace: String? = null,
    val obsPlaceDetail: String? = null,
    val flowerStatus: String? = null,
    val bfShotDate: String? = null,
    val cfShotDate: String? = null,
    val ffShotDate: String? = null,
    val modDate: String? = null,
)
