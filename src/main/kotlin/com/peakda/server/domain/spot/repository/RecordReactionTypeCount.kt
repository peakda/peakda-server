package com.peakda.server.domain.spot.repository

/** [SpotRecordReactionRepository.countsBySpotRecordIdIn] 프로젝션. */
interface RecordReactionTypeCount {
    val spotRecordId: Long
    val reactionType: String
    val count: Long
}
