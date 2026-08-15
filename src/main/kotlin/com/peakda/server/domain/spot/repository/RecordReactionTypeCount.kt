package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.ReactionType

/** [SpotRecordReactionRepository.countsBySpotRecordIdIn] 프로젝션. */
interface RecordReactionTypeCount {
    val spotRecordId: Long
    val reactionType: ReactionType
    val count: Long
}
