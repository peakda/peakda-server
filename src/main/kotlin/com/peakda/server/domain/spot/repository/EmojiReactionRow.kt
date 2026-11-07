package com.peakda.server.domain.spot.repository

interface EmojiReactionRow {
    val storageKey: String
    val count: Long
    val reactedByMe: Boolean
}
