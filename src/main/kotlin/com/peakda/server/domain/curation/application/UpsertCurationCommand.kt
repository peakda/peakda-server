package com.peakda.server.domain.curation.application

import com.peakda.server.domain.curation.entity.CurationStatus
import java.time.LocalDate

data class UpsertCurationCommand(
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val weekLabel: String,
    val heroImageUrl: String?,
    val title: String,
    val subtitle: String?,
    val intro: String?,
    val nextTeaserOverline: String?,
    val nextTeaserBody: String?,
    val status: CurationStatus,
    val chapters: List<UpsertCurationChapterCommand>,
    val recommendations: List<UpsertCurationRecommendationCommand>,
)
