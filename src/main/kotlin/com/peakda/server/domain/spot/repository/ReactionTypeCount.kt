package com.peakda.server.domain.spot.repository

/** [SpotRecordReactionRepository.countsBySpotRecordId] 프로젝션. */
interface ReactionTypeCount {
    val reactionType: String
    val count: Long
}
