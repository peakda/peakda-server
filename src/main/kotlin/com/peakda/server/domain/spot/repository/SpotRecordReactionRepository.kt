package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.SpotRecordReaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpotRecordReactionRepository : JpaRepository<SpotRecordReaction, Long> {
    @Query("SELECT r FROM SpotRecordReaction r WHERE r.spotRecordId = :recordId AND r.userId = :userId AND r.reactionType IN ('HEART', 'SMILE')")
    fun findBySpotRecordIdAndUserId(@Param("recordId") recordId: Long, @Param("userId") userId: Long): List<SpotRecordReaction>

    @Query("DELETE FROM SpotRecordReaction r WHERE r.userId = :userId AND r.spotRecordId = :recordId AND r.reactionType = :reactionType")
    @Modifying
    fun deleteByUserIdAndSpotRecordIdAndReactionType(@Param("userId") userId: Long, @Param("recordId") spotRecordId: Long, @Param("reactionType") reactionType: String)

    @Query("SELECT r.reactionType AS reactionType, COUNT(r) AS count FROM SpotRecordReaction r WHERE r.spotRecordId = :spotRecordId AND r.reactionType IN ('HEART', 'SMILE') GROUP BY r.reactionType")
    fun countsBySpotRecordId(@Param("spotRecordId") spotRecordId: Long): List<ReactionTypeCount>

    @Query("SELECT r.spotRecordId AS spotRecordId, r.reactionType AS reactionType, COUNT(r) AS count FROM SpotRecordReaction r WHERE r.spotRecordId IN :spotRecordIds AND r.reactionType IN ('HEART', 'SMILE') GROUP BY r.spotRecordId, r.reactionType")
    fun countsBySpotRecordIdIn(@Param("spotRecordIds") spotRecordIds: List<Long>): List<RecordReactionTypeCount>

    @Query("SELECT r FROM SpotRecordReaction r WHERE r.userId = :userId AND r.spotRecordId IN :recordIds AND r.reactionType IN ('HEART', 'SMILE')")
    fun findByUserIdAndSpotRecordIdIn(@Param("userId") userId: Long, @Param("recordIds") recordIds: List<Long>): List<SpotRecordReaction>

    @Query("SELECT COUNT(r) FROM SpotRecordReaction r WHERE r.spotRecordId = :recordId AND r.reactionType = :storageKey")
    fun countByRecordAndStorageKey(recordId: Long, storageKey: String): Long

    @Query("SELECT COUNT(r) > 0 FROM SpotRecordReaction r WHERE r.spotRecordId = :recordId AND r.userId = :userId AND r.reactionType = :storageKey")
    fun existsByRecordUserAndStorageKey(recordId: Long, userId: Long, storageKey: String): Boolean

    @Query(
        value = """
            WITH grouped AS (
                SELECT r.spot_record_id AS spotRecordId, r.reaction_type AS storageKey,
                       COUNT(*) AS count,
                       (CAST(:userId AS bigint) IS NOT NULL AND BOOL_OR(r.user_id = CAST(:userId AS bigint))) AS reactedByMe
                FROM spot_record_reactions r
                WHERE r.spot_record_id IN (:recordIds)
                GROUP BY r.spot_record_id, r.reaction_type
            ), ranked AS (
                SELECT grouped.*, ROW_NUMBER() OVER (PARTITION BY spotRecordId ORDER BY count DESC, CASE storageKey WHEN 'HEART' THEN 'u:2764-fe0f' WHEN 'SMILE' THEN 'u:1f600' ELSE storageKey END) AS rank
                FROM grouped
            )
            SELECT spotRecordId, storageKey, count, reactedByMe FROM ranked WHERE rank <= 9 ORDER BY spotRecordId, rank
        """,
        nativeQuery = true,
    )
    fun emojiPreviews(@Param("recordIds") recordIds: List<Long>, @Param("userId") userId: Long?): List<EmojiReactionCount>

    @Query(
        value = """
            SELECT r.reaction_type AS storageKey, COUNT(*) AS count,
                   (CAST(:userId AS bigint) IS NOT NULL AND BOOL_OR(r.user_id = CAST(:userId AS bigint))) AS reactedByMe
            FROM spot_record_reactions r
            WHERE r.spot_record_id = :recordId
              AND (CAST(:afterKey AS text) IS NULL OR CASE r.reaction_type WHEN 'HEART' THEN 'u:2764-fe0f' WHEN 'SMILE' THEN 'u:1f600' ELSE r.reaction_type END > CAST(:afterKey AS text))
            GROUP BY r.reaction_type
            HAVING CAST(:mineOnly AS boolean) = FALSE OR (CAST(:userId AS bigint) IS NOT NULL AND BOOL_OR(r.user_id = CAST(:userId AS bigint)))
            ORDER BY CASE r.reaction_type WHEN 'HEART' THEN 'u:2764-fe0f' WHEN 'SMILE' THEN 'u:1f600' ELSE r.reaction_type END
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun emojiList(@Param("recordId") recordId: Long, @Param("userId") userId: Long?, @Param("afterKey") afterKey: String?, @Param("mineOnly") mineOnly: Boolean, @Param("limit") limit: Int): List<EmojiReactionRow>

    @Modifying
    @Query(
        value = "INSERT INTO spot_record_reactions (user_id, spot_record_id, reaction_type, created_at, updated_at) VALUES (:userId, :spotRecordId, :reactionType, now(), now()) ON CONFLICT (user_id, spot_record_id, reaction_type) DO NOTHING",
        nativeQuery = true,
    )
    fun insertIfAbsent(userId: Long, spotRecordId: Long, reactionType: String): Int
}
