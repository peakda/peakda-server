package com.peakda.server.domain.curation.application

import com.peakda.server.domain.curation.entity.CurationLayout

data class UpsertCurationChapterCommand(
    val layout: CurationLayout,
    val heading: String,
    val spotId: Long?,
    val placeName: String,
    val latitude: Double?,
    val longitude: Double?,
    val photoUrl: String?,
    val pullQuote: String?,
    val leadText: String?,
    val body: String,
    val factNote: String?,
)
