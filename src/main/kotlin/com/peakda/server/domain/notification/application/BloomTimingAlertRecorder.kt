package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.repository.BloomTimingAlertRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * dedup 로그와 사용자 알림을 같은 트랜잭션에 원자적으로 기록한다.
 */
@Service
@Transactional
class BloomTimingAlertRecorder(
    private val bloomTimingAlertRepository: BloomTimingAlertRepository,
    private val notificationService: NotificationService,
) {

    fun record(candidate: BloomTimingAlertCandidate): Notification? {
        val inserted = bloomTimingAlertRepository.insertIfAbsent(
            userId = candidate.userId,
            spotId = candidate.spotId,
            bloomCategory = candidate.bloomCategory.name,
            peakYear = candidate.peakYear,
            peakStartDate = candidate.peakStartDate,
        )
        if (inserted == 0) return null

        return notificationService.create(
            CreateNotificationCommand(
                recipientId = candidate.userId,
                type = NotificationType.TIMING,
                title = BloomTimingAlertMessage.TITLE,
                body = BloomTimingAlertMessage.body(candidate),
                linkType = NotificationLinkType.INTERNAL,
                targetId = candidate.spotId,
            ),
        )
    }
}
