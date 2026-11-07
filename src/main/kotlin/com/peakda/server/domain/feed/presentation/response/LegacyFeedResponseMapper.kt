package com.peakda.server.domain.feed.presentation.response

import com.peakda.server.domain.spot.application.SpotRecordView
import com.peakda.server.domain.spot.application.LegacyReactionOverview
import com.peakda.server.domain.spot.application.PhotoView
import com.peakda.server.domain.spot.application.SpotView
import com.peakda.server.domain.spot.application.UserView
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.entity.ReactionType

fun SpotRecordView.toLegacyFeedRecordResponse(): LegacyFeedRecordResponse = LegacyFeedRecordResponse(
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
    reactions = legacyReactions.toResponse(),
)

fun SpotRecordView.toLegacyFeedRecordSummaryResponse(): LegacyFeedRecordSummaryResponse {
    val mappedPhotos = photos.map { it.toResponse() }
    return LegacyFeedRecordSummaryResponse(
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
        reactions = legacyReactions.toResponse(),
    )
}

private fun SpotView.toResponse() = SpotRecordResponse.SpotSummary(id, type, name, address, attractionId)
private fun UserView.toResponse() = SpotRecordResponse.UserSummary(id, nickname, profileImageUrl)
private fun PhotoView.toResponse() = SpotRecordResponse.PhotoEntry(objectKey, url, sortOrder, variants)

private fun LegacyReactionOverview.toResponse() = ReactionSummary(
    counts = counts.mapNotNull { count ->
        ReactionType.entries.firstOrNull { it.name == count.reactionType }
            ?.let { ReactionCount(it, count.count) }
    },
    myReactions = myReactions.mapNotNull { key -> ReactionType.entries.firstOrNull { it.name == key } }.toSet(),
)
