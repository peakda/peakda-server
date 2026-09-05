package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.repository.BloomTimingAlertRepository
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.AlertTargetFavorite
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.infrastructure.push.PushPayload
import com.peakda.server.infrastructure.push.PushSender
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 만개 임박 알림 오케스트레이션. 알림 대상 찜(spot 도메인)과 창 안 개화 추정(seasonal 도메인)을
 * 각 도메인 레포에서 따로 조회해 명소 id 로 매칭하고, "만개 임박"(오늘+1..오늘+leadDays, ENDED 제외) 판정을
 * 이 서비스가 소유한다. 새로 기록된 알림만 트랜잭션 밖에서, 페이지당 토큰을 한 번에 조회해 푸시한다.
 */
@Service
class BloomTimingAlertService(
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val bloomTimingAlertRepository: BloomTimingAlertRepository,
    private val pushSender: PushSender,
    private val recorder: BloomTimingAlertRecorder,
    private val props: BloomTimingAlertProperties,
) {

    fun sendDueAlerts(today: LocalDate): Int {
        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate() ?: return 0
        val windowStart = today.plusDays(1)
        val windowEnd = today.plusDays(props.leadDays)
        var sent = 0
        var page = 0

        while (true) {
            val slice = spotFavoriteRepository.findAlertTargets(
                attractionType = SpotType.ATTRACTION,
                pageable = PageRequest.of(page, props.pageSize),
            )
            if (slice.content.isEmpty()) break
            sent += dispatch(slice.content, baseDate, windowStart, windowEnd, today)
            if (!slice.hasNext()) break
            page++
        }
        return sent
    }

    /**
     * 한 페이지의 알림 대상 찜을 처리한다. 창 안 추정과 토큰을 각각 한 번에 조회해
     * 후보마다 재조회하지 않고, 명소 id 로 매칭한 뒤 새로 기록된 알림만 푸시한다.
     */
    private fun dispatch(
        favorites: List<AlertTargetFavorite>,
        baseDate: LocalDate,
        windowStart: LocalDate,
        windowEnd: LocalDate,
        today: LocalDate,
    ): Int {
        val attractionIds = favorites.map { it.attractionId }.distinct()
        val estimatesByAttraction = seasonalBloomEstimateRepository
            .findByBaseDateAndAttractionIdInAndStatusNotAndPeakStartDateBetween(
                baseDate = baseDate,
                attractionIds = attractionIds,
                status = BloomStatus.ENDED,
                peakStartDateStart = windowStart,
                peakStartDateEnd = windowEnd,
            )
            .groupBy { it.attractionId }
        val tokensByUser = deviceTokenRepository
            .findByUserIdIn(favorites.map { it.userId }.distinct())
            .groupBy { it.userId }

        var count = 0
        for (favorite in favorites) {
            val estimates = estimatesByAttraction[favorite.attractionId] ?: continue
            for (estimate in estimates) {
                val candidate = candidate(favorite, estimate, today) ?: continue
                val notification = recorder.record(candidate) ?: continue

                val tokens = tokensByUser[favorite.userId].orEmpty()
                if (tokens.isNotEmpty()) {
                    val notificationId = requireNotNull(notification.id)
                    pushSender.send(
                        tokens,
                        PushPayload(
                            title = BloomTimingAlertMessage.TITLE,
                            body = BloomTimingAlertMessage.body(candidate),
                            linkType = NotificationLinkType.INTERNAL,
                            linkUrl = null,
                            targetId = candidate.spotId,
                            notificationId = notificationId,
                            type = notification.type,
                        ),
                    )
                }
                count++
            }
        }
        return count
    }

    private fun candidate(
        favorite: AlertTargetFavorite,
        estimate: SeasonalBloomEstimate,
        today: LocalDate,
    ): BloomTimingAlertCandidate? {
        val peakStart = estimate.peakStartDate ?: return null
        return BloomTimingAlertCandidate(
            userId = favorite.userId,
            spotId = favorite.spotId,
            spotName = favorite.spotName,
            bloomCategory = estimate.bloomCategory,
            peakStartDate = peakStart,
            daysUntilPeak = ChronoUnit.DAYS.between(today, peakStart),
        )
    }

    /** 계정 탈퇴 시 만개 임박 알림 중복 방지 로그를 함께 정리한다. */
    @Transactional
    fun deleteAllByUser(userId: Long) {
        bloomTimingAlertRepository.deleteByUserId(userId)
    }
}
