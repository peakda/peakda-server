package com.peakda.server.domain.feed.application

import com.peakda.server.domain.feed.presentation.response.FeedReactionSummaryResponse
import com.peakda.server.domain.feed.presentation.response.ReactionCount
import com.peakda.server.domain.spot.entity.ReactionType
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 피드 리액션(SCR-023, 결정 F) — 고정 이모지 2종(HEART/SMILE)을 게시된 기록에 토글한다. 댓글은 V1 범위 밖.
 */
@Service
@Transactional
class FeedReactionService(
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordReactionRepository: SpotRecordReactionRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    fun add(userId: Long, recordId: Long, reactionType: ReactionType): FeedReactionSummaryResponse {
        val record = requirePublished(recordId)
        spotRecordReactionRepository.insertIfAbsent(userId, recordId, reactionType.name)
        // 커밋 후 알림 도메인이 수신해 기록 작성자에게 리액션 알림을 생성한다 (본인 리액션은 리스너에서 생략).
        eventPublisher.publishEvent(ReactionAddedEvent(userId, recordId, record.userId, reactionType))
        return summary(recordId, userId)
    }

    fun remove(userId: Long, recordId: Long, reactionType: ReactionType): FeedReactionSummaryResponse {
        requirePublished(recordId)
        spotRecordReactionRepository.deleteByUserIdAndSpotRecordIdAndReactionType(userId, recordId, reactionType)
        return summary(recordId, userId)
    }

    /** 게시된 기록만 리액션 대상으로 허용 — DRAFT 존재 자체를 숨기려 404 로 통일한다. */
    private fun requirePublished(recordId: Long): SpotRecord {
        val record = spotRecordRepository.findById(recordId).orElseThrow { SpotRecordNotFoundException() }
        if (record.status != SpotRecordStatus.PUBLISHED) throw SpotRecordNotFoundException()
        return record
    }

    @Transactional(readOnly = true)
    fun summary(recordId: Long, userId: Long): FeedReactionSummaryResponse {
        val counts = spotRecordReactionRepository.countsBySpotRecordId(recordId)
            .map { ReactionCount(it.reactionType, it.count) }
        val mine = spotRecordReactionRepository.findBySpotRecordIdAndUserId(recordId, userId)
            .map { it.reactionType }
            .toSet()
        return FeedReactionSummaryResponse(recordId = recordId, counts = counts, myReactions = mine)
    }
}
