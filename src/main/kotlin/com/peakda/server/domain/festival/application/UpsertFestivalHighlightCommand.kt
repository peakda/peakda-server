package com.peakda.server.domain.festival.application

data class UpsertFestivalHighlightCommand(
    val title: String,
    val body: String,
)
