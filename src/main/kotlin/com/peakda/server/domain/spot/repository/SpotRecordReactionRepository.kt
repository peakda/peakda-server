package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.SpotRecordReaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpotRecordReactionRepository : JpaRepository<SpotRecordReaction, Long> {
    fun findBySpotRecordIdAndUserId(spotRecordId: Long, userId: Long): List<SpotRecordReaction>
    fun deleteByUserIdAndSpotRecordIdAndReactionType(userId: Long, spotRecordId: Long, reactionType: ReactionType)

    /** 기록 하나의 리액션 타입별 집계. */
    @Query(
        """
            SELECT r.reactionType AS reactionType, COUNT(r) AS count
            FROM SpotRecordReaction r
            WHERE r.spotRecordId = :spotRecordId
            GROUP BY r.reactionType
        """,
    )
    fun countsBySpotRecordId(@Param("spotRecordId") spotRecordId: Long): List<ReactionTypeCount>

    /** 여러 기록의 리액션 타입별 집계. 목록 조회에서 기록별 N+1을 방지한다. */
    @Query(
        """
            SELECT r.spotRecordId AS spotRecordId, r.reactionType AS reactionType, COUNT(r) AS count
            FROM SpotRecordReaction r
            WHERE r.spotRecordId IN :spotRecordIds
            GROUP BY r.spotRecordId, r.reactionType
        """,
    )
    fun countsBySpotRecordIdIn(@Param("spotRecordIds") spotRecordIds: List<Long>): List<RecordReactionTypeCount>

    fun findByUserIdAndSpotRecordIdIn(userId: Long, spotRecordIds: List<Long>): List<SpotRecordReaction>

    /**
     * 리액션을 멱등하게 추가한다. 이미 같은 (user, record, type) 이 있으면 무시되므로
     * 동시 요청에서도 유니크 제약 위반 예외 없이 단일 행을 보장한다.
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO spot_record_reactions (user_id, spot_record_id, reaction_type, created_at, updated_at)
            VALUES (:userId, :spotRecordId, :reactionType, now(), now())
            ON CONFLICT (user_id, spot_record_id, reaction_type) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(userId: Long, spotRecordId: Long, reactionType: String)
}
