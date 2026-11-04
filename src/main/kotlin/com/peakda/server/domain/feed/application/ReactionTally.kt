package com.peakda.server.domain.feed.application

data class ReactionTally(
    val key: String,
    val emoji: String,
    val count: Long,
    val reactedByMe: Boolean,
)
