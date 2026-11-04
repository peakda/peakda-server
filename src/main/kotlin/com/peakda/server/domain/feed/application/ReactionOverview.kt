package com.peakda.server.domain.feed.application

data class ReactionOverview(
    val items: List<ReactionTally>,
    val hasMore: Boolean,
)
