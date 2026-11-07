package com.peakda.server.domain.feed.presentation.response

import com.peakda.server.domain.feed.application.EmojiCatalog
import com.peakda.server.domain.feed.application.ReactionOverview
import com.peakda.server.domain.spot.entity.ReactionType

fun ReactionOverview.toLegacyResponse(recordId: Long, catalog: EmojiCatalog): FeedReactionSummaryResponse {
    val legacyTypes = ReactionType.entries.associateBy { catalog.legacyKey(it) }
    return FeedReactionSummaryResponse(
        recordId = recordId,
        counts = items.mapNotNull { item -> legacyTypes[item.key]?.let { ReactionCount(it, item.count) } },
        myReactions = items.filter { it.reactedByMe }.mapNotNull { legacyTypes[it.key] }.toSet(),
    )
}
