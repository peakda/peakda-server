package com.peakda.server.domain.feed.application

import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FeedReactionService(
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordReactionRepository: SpotRecordReactionRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val emojiCatalog: EmojiCatalog,
) {
    fun add(userId: Long, recordId: Long, emojiKey: String): ReactionState {
        val record = requirePublished(recordId)
        val definition = emojiCatalog.require(emojiKey)
        val storageKey = emojiCatalog.toStorageKey(definition.key)
        if (spotRecordReactionRepository.insertIfAbsent(userId, recordId, storageKey) > 0) {
            eventPublisher.publishEvent(ReactionAddedEvent(userId, recordId, record.userId, definition.key))
        }
        return state(recordId, userId, definition)
    }

    fun remove(userId: Long, recordId: Long, emojiKey: String): ReactionState {
        requirePublished(recordId)
        val definition = emojiCatalog.require(emojiKey)
        spotRecordReactionRepository.deleteByUserIdAndSpotRecordIdAndReactionType(userId, recordId, emojiCatalog.toStorageKey(definition.key))
        return state(recordId, userId, definition)
    }

    @Transactional(readOnly = true)
    fun legacySummary(recordId: Long, userId: Long?): ReactionOverview {
        val counts = spotRecordReactionRepository.countsBySpotRecordId(recordId).associate { it.reactionType to it.count }
        val mine = userId?.let { spotRecordReactionRepository.findBySpotRecordIdAndUserId(recordId, it).map { row -> row.reactionType }.toSet() }.orEmpty()
        val items = ReactionType.entries.mapNotNull { type ->
            val key = emojiCatalog.legacyKey(type)
            val definition = emojiCatalog.require(key)
            val storage = emojiCatalog.toStorageKey(key)
            val count = counts[storage] ?: 0L
            if (count == 0L && storage !in mine) null else ReactionTally(key, definition.emoji, count, storage in mine)
        }
        return ReactionOverview(items, false)
    }

    @Transactional(readOnly = true)
    fun previews(recordIds: List<Long>, userId: Long?): Map<Long, ReactionOverview> {
        if (recordIds.isEmpty()) return emptyMap()
        val grouped = spotRecordReactionRepository.emojiPreviews(recordIds, userId).groupBy { it.spotRecordId }
        return recordIds.associateWith { id ->
            val rows = grouped[id].orEmpty()
            ReactionOverview(rows.take(8).map { tally(it.storageKey, it.count, it.reactedByMe) }, rows.size > 8)
        }
    }

    @Transactional(readOnly = true)
    fun list(recordId: Long, userId: Long?, afterKey: String?, size: Int, mineOnly: Boolean): ReactionPage {
        requirePublished(recordId)
        if (size !in 1..100) throw InvalidEmojiException()
        val after = afterKey?.let { emojiCatalog.require(it).key }
        val rows = spotRecordReactionRepository.emojiList(recordId, userId, after, mineOnly, size + 1)
        val items = rows.take(size).map { tally(it.storageKey, it.count, it.reactedByMe) }
        val hasMore = rows.size > size
        return ReactionPage(items, hasMore, if (hasMore) items.lastOrNull()?.key else null)
    }

    private fun state(recordId: Long, userId: Long, definition: EmojiDefinition): ReactionState {
        val storage = emojiCatalog.toStorageKey(definition.key)
        return ReactionState(
            recordId = recordId,
            key = definition.key,
            emoji = definition.emoji,
            count = spotRecordReactionRepository.countByRecordAndStorageKey(recordId, storage),
            reactedByMe = spotRecordReactionRepository.existsByRecordUserAndStorageKey(recordId, userId, storage),
        )
    }

    private fun tally(storageKey: String, count: Long, reactedByMe: Boolean): ReactionTally {
        val definition = emojiCatalog.fromStorageKey(storageKey)
        return ReactionTally(definition.key, definition.emoji, count, reactedByMe)
    }

    private fun requirePublished(recordId: Long): SpotRecord {
        val record = spotRecordRepository.findById(recordId).orElseThrow { SpotRecordNotFoundException() }
        if (record.status != SpotRecordStatus.PUBLISHED) throw SpotRecordNotFoundException()
        return record
    }
}
