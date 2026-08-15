package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.ReactionType

/** [SpotRecordReactionRepository.countsBySpotRecordId] 프로젝션. */
interface ReactionTypeCount {
    val reactionType: ReactionType
    val count: Long
}
