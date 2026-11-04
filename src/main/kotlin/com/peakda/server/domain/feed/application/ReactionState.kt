package com.peakda.server.domain.feed.application

data class ReactionState(
    val recordId: Long,
    val key: String,
    val emoji: String,
    val count: Long,
    val reactedByMe: Boolean,
)
