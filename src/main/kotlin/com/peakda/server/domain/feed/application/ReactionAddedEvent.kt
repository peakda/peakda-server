package com.peakda.server.domain.feed.application

import com.peakda.server.domain.spot.entity.ReactionType

/**
 * 게시된 기록에 리액션이 추가됐을 때 발행되는 도메인 이벤트. 알림 도메인이 AFTER_COMMIT 으로 수신해
 * 기록 작성자([recordOwnerId])에게 리액션 알림을 생성한다. 본인이 남긴 리액션이면 알림은 생략된다.
 */
data class ReactionAddedEvent(
    val actorId: Long,
    val recordId: Long,
    val recordOwnerId: Long,
    val reactionType: ReactionType,
)
