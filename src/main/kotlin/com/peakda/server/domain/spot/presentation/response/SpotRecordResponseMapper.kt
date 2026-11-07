package com.peakda.server.domain.spot.presentation.response

import com.peakda.server.domain.feed.application.ReactionOverview
import com.peakda.server.domain.feed.presentation.v2.response.toResponse
import com.peakda.server.domain.spot.application.SpotRecordView
import com.peakda.server.domain.spot.application.PhotoView
import com.peakda.server.domain.spot.application.SpotView
import com.peakda.server.domain.spot.application.UserView

fun SpotRecordView.toRecordResponse(reactions: ReactionOverview): SpotRecordResponse = SpotRecordResponse(
    id = id,
    spot = spot.toResponse(),
    user = user.toResponse(),
    visitedDate = visitedDate,
    bloomStage = bloomStage,
    memo = memo,
    contentLink = contentLink?.let { SpotRecordResponse.ContentLink(it.url, it.platform) },
    plants = plants.map { SpotRecordResponse.PlantSummary(it.id, it.name) },
    photos = photos.map { it.toResponse() },
    status = status,
    publishedAt = publishedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reactions = reactions.toResponse(),
)

fun SpotRecordView.toRecordSummaryResponse(reactions: ReactionOverview): SpotRecordSummaryResponse {
    val mappedPhotos = photos.map { it.toResponse() }
    return SpotRecordSummaryResponse(
        id = id,
        spotId = spot.id,
        spotName = spot.name,
        user = user.toResponse(),
        visitedDate = visitedDate,
        bloomStage = bloomStage,
        memo = memo,
        contentLink = contentLink?.let { SpotRecordResponse.ContentLink(it.url, it.platform) },
        plants = plants.map { SpotRecordResponse.PlantSummary(it.id, it.name) },
        coverPhoto = mappedPhotos.firstOrNull(),
        photos = mappedPhotos,
        status = status,
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        reactions = reactions.toResponse(),
    )
}

private fun SpotView.toResponse() = SpotRecordResponse.SpotSummary(id, type, name, address, attractionId)
private fun UserView.toResponse() = SpotRecordResponse.UserSummary(id, nickname, profileImageUrl)
private fun PhotoView.toResponse() = SpotRecordResponse.PhotoEntry(objectKey, url, sortOrder, variants)
