package com.peakda.server.domain.feed.presentation.v2.response

import com.peakda.server.domain.feed.application.ReactionOverview
import com.peakda.server.domain.feed.application.ReactionPage
import com.peakda.server.domain.feed.application.ReactionState
import com.peakda.server.domain.feed.application.ReactionTally
import com.peakda.server.domain.spot.application.SpotRecordView

fun ReactionTally.toResponse() = ReactionItemResponse(key, emoji, count, reactedByMe)
fun ReactionOverview.toResponse() = ReactionOverviewResponse(items.map { it.toResponse() }, hasMore)
fun ReactionState.toResponse() = ReactionStateResponse(recordId, key, emoji, count, reactedByMe)
fun ReactionPage.toResponse() = ReactionPageResponse(items.map { it.toResponse() }, hasMore, nextAfterKey)

fun SpotRecordView.toFeedRecordResponse(reactions: ReactionOverview) = FeedRecordResponse(
    id = id,
    spot = FeedRecordResponse.SpotSummary(spot.id, spot.type, spot.name, spot.address, spot.attractionId),
    user = FeedRecordResponse.UserSummary(user.id, user.nickname, user.profileImageUrl),
    visitedDate = visitedDate,
    bloomStage = bloomStage,
    memo = memo,
    contentLink = contentLink?.let { FeedRecordResponse.ContentLink(it.url, it.platform) },
    plants = plants.map { FeedRecordResponse.PlantSummary(it.id, it.name) },
    photos = photos.map { FeedRecordResponse.PhotoEntry(it.objectKey, it.url, it.sortOrder, it.variants) },
    status = status,
    publishedAt = publishedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reactions = reactions.toResponse(),
)

fun SpotRecordView.toFeedRecordSummaryResponse(reactions: ReactionOverview): FeedRecordSummaryResponse {
    val photoEntries = photos.map { FeedRecordResponse.PhotoEntry(it.objectKey, it.url, it.sortOrder, it.variants) }
    return FeedRecordSummaryResponse(
        id = id,
        spotId = spot.id,
        spotName = spot.name,
        user = FeedRecordResponse.UserSummary(user.id, user.nickname, user.profileImageUrl),
        visitedDate = visitedDate,
        bloomStage = bloomStage,
        memo = memo,
        contentLink = contentLink?.let { FeedRecordResponse.ContentLink(it.url, it.platform) },
        plants = plants.map { FeedRecordResponse.PlantSummary(it.id, it.name) },
        coverPhoto = photoEntries.firstOrNull(),
        photos = photoEntries,
        status = status,
        publishedAt = publishedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        reactions = reactions.toResponse(),
    )
}
