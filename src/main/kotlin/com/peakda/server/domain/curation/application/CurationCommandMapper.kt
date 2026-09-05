package com.peakda.server.domain.curation.application

import com.peakda.server.domain.curation.presentation.request.UpsertCurationRequest

fun UpsertCurationRequest.toCommand(): UpsertCurationCommand = UpsertCurationCommand(
    weekStartDate = weekStartDate,
    weekEndDate = weekEndDate,
    weekLabel = weekLabel,
    heroImageUrl = heroImageUrl,
    title = title,
    subtitle = subtitle,
    intro = intro,
    nextTeaserOverline = nextTeaserOverline,
    nextTeaserBody = nextTeaserBody,
    status = status,
    chapters = chapters.map { chapter ->
        UpsertCurationChapterCommand(
            layout = chapter.layout,
            heading = chapter.heading,
            spotId = chapter.spotId,
            placeName = chapter.placeName,
            latitude = chapter.latitude,
            longitude = chapter.longitude,
            photoUrl = chapter.photoUrl,
            pullQuote = chapter.pullQuote,
            leadText = chapter.leadText,
            body = chapter.body,
            factNote = chapter.factNote,
        )
    },
    recommendations = recommendations.map { recommendation ->
        UpsertCurationRecommendationCommand(
            title = recommendation.title,
            spotId = recommendation.spotId,
            placeName = recommendation.placeName,
            latitude = recommendation.latitude,
            longitude = recommendation.longitude,
            photoUrl = recommendation.photoUrl,
            body = recommendation.body,
        )
    },
)
