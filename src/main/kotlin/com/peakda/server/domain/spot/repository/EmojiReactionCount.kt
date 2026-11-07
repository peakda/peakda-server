package com.peakda.server.domain.spot.repository

interface EmojiReactionCount {
    val spotRecordId: Long
    val storageKey: String
    val count: Long
    val reactedByMe: Boolean
}
