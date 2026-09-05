package com.peakda.server.domain.notification.application

/**
 * 저장 알림과 푸시가 동일한 카피를 사용하도록 만드는 만개 임박 알림 메시지 정본.
 */
object BloomTimingAlertMessage {
    const val TITLE = "곧 만개해요 🌸"

    fun body(candidate: BloomTimingAlertCandidate): String =
        "${candidate.spotName}의 ${candidate.bloomCategory.displayName} 만개까지 " +
            "${candidate.daysUntilPeak}일 남았어요. 방문 계획을 세워보세요!"
}
