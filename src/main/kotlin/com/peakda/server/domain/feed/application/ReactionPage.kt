package com.peakda.server.domain.feed.application

data class ReactionPage(
    val items: List<ReactionTally>,
    val hasMore: Boolean,
    val nextAfterKey: String?,
)
