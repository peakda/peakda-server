package com.peakda.server.domain.user.application

/**
 * 팔로우가 새로 맺어졌을 때 발행되는 도메인 이벤트. 알림 도메인이 AFTER_COMMIT 으로 수신해 팔로우 알림을 생성한다.
 */
data class FollowCreatedEvent(
    val followerId: Long,
    val followingId: Long,
)
