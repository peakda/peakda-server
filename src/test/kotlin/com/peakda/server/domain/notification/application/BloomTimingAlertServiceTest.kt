package com.peakda.server.domain.notification.application

import com.peakda.server.domain.notification.entity.DevicePlatform
import com.peakda.server.domain.notification.entity.DeviceToken
import com.peakda.server.domain.notification.entity.Notification
import com.peakda.server.domain.notification.entity.NotificationLinkType
import com.peakda.server.domain.notification.entity.NotificationType
import com.peakda.server.domain.notification.repository.BloomTimingAlertRepository
import com.peakda.server.domain.notification.repository.DeviceTokenRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.AlertTargetFavorite
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.infrastructure.push.PushPayload
import com.peakda.server.infrastructure.push.PushSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.SliceImpl
import java.time.LocalDate

class BloomTimingAlertServiceTest {

    private val spotFavoriteRepository = mock(SpotFavoriteRepository::class.java)
    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)
    private val deviceTokenRepository = mock(DeviceTokenRepository::class.java)
    private val bloomTimingAlertRepository = mock(BloomTimingAlertRepository::class.java)
    private val pushSender = mock(PushSender::class.java)
    private val recorder = mock(BloomTimingAlertRecorder::class.java)
    private val props = BloomTimingAlertProperties(leadDays = 7, pageSize = 2)
    private val service = BloomTimingAlertService(
        spotFavoriteRepository = spotFavoriteRepository,
        seasonalBloomEstimateRepository = seasonalBloomEstimateRepository,
        deviceTokenRepository = deviceTokenRepository,
        bloomTimingAlertRepository = bloomTimingAlertRepository,
        pushSender = pushSender,
        recorder = recorder,
        props = props,
    )

    private val candidateCaptor: ArgumentCaptor<BloomTimingAlertCandidate> =
        ArgumentCaptor.forClass(BloomTimingAlertCandidate::class.java)

    @Test
    fun `최신 추정 기준일이 없으면 찜 대상을 조회하지 않고 0을 반환한다`() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(null)

        assertThat(service.sendDueAlerts(TODAY)).isZero()

        verifyNoInteractions(spotFavoriteRepository, deviceTokenRepository, recorder, pushSender)
    }

    @Test
    fun `명소형 찜과 창 안 추정과 토큰이 있으면 신규 후보를 기록하고 푸시한다`() {
        val favorite = favorite()
        val estimate = estimate(peakStartDate = TODAY.plusDays(7))
        val tokens = listOf(DeviceToken(1L, "token-1", DevicePlatform.ANDROID))
        arrangePage(page = 0, favorites = listOf(favorite))
        arrangeEstimates(favorites = listOf(favorite), estimates = listOf(estimate))
        `when`(deviceTokenRepository.findByUserIdIn(listOf(1L))).thenReturn(tokens)
        `when`(recorder.record(anyCandidate())).thenReturn(notification())

        val sent = service.sendDueAlerts(TODAY)

        assertThat(sent).isEqualTo(1)
        verify(deviceTokenRepository, times(1)).findByUserIdIn(listOf(1L))
        verify(recorder).record(captureCandidate())
        assertThat(candidateCaptor.value.daysUntilPeak).isEqualTo(7)
        verify(pushSender).send(
            tokens,
            PushPayload(
                title = BloomTimingAlertMessage.TITLE,
                body = BloomTimingAlertMessage.body(candidateCaptor.value),
                linkType = NotificationLinkType.INTERNAL,
                linkUrl = null,
                targetId = 10L,
            ),
        )
    }

    @Test
    fun `이미 기록된 후보면 푸시하지 않고 카운트를 올리지 않는다`() {
        val favorite = favorite()
        arrangePage(page = 0, favorites = listOf(favorite))
        arrangeEstimates(favorites = listOf(favorite), estimates = listOf(estimate()))
        `when`(deviceTokenRepository.findByUserIdIn(listOf(1L))).thenReturn(emptyList())
        `when`(recorder.record(anyCandidate())).thenReturn(null)

        assertThat(service.sendDueAlerts(TODAY)).isZero()

        verify(recorder).record(anyCandidate())
        verify(pushSender, never()).send(anyTokenList(), anyPayload())
    }

    @Test
    fun `토큰이 없으면 기록은 유지하고 푸시만 생략한다`() {
        val favorite = favorite()
        arrangePage(page = 0, favorites = listOf(favorite))
        arrangeEstimates(favorites = listOf(favorite), estimates = listOf(estimate()))
        `when`(deviceTokenRepository.findByUserIdIn(listOf(1L))).thenReturn(emptyList())
        `when`(recorder.record(anyCandidate())).thenReturn(notification())

        assertThat(service.sendDueAlerts(TODAY)).isEqualTo(1)

        verify(recorder).record(anyCandidate())
        verify(deviceTokenRepository).findByUserIdIn(listOf(1L))
        verifyNoInteractions(pushSender)
    }

    @Test
    fun `찜의 명소와 일치하는 추정이 없으면 기록과 푸시를 생략한다`() {
        val favorite = favorite(attractionId = 100L)
        arrangePage(page = 0, favorites = listOf(favorite))
        arrangeEstimates(
            favorites = listOf(favorite),
            estimates = listOf(estimate(attractionId = 200L)),
        )
        `when`(deviceTokenRepository.findByUserIdIn(listOf(1L))).thenReturn(emptyList())

        assertThat(service.sendDueAlerts(TODAY)).isZero()

        verifyNoInteractions(recorder, pushSender)
    }

    @Test
    fun `한 명소에 카테고리 두 개가 있으면 각각 기록하고 푸시한다`() {
        val favorite = favorite()
        val estimates = listOf(
            estimate(bloomCategory = BloomCategory.CHERRY),
            estimate(bloomCategory = BloomCategory.PLUM),
        )
        val tokens = listOf(DeviceToken(1L, "token-1", DevicePlatform.ANDROID))
        arrangePage(page = 0, favorites = listOf(favorite))
        arrangeEstimates(favorites = listOf(favorite), estimates = estimates)
        `when`(deviceTokenRepository.findByUserIdIn(listOf(1L))).thenReturn(tokens)
        `when`(recorder.record(anyCandidate())).thenReturn(notification())

        assertThat(service.sendDueAlerts(TODAY)).isEqualTo(2)

        verify(recorder, times(2)).record(captureCandidate())
        assertThat(candidateCaptor.allValues.map { it.bloomCategory })
            .containsExactly(BloomCategory.CHERRY, BloomCategory.PLUM)
        verify(pushSender, times(2)).send(anyTokenList(), anyPayload())
    }

    @Test
    fun `다음 slice가 있으면 마지막 페이지까지 순회한다`() {
        val first = favorite(userId = 1L, spotId = 10L, attractionId = 100L)
        val second = favorite(userId = 2L, spotId = 20L, attractionId = 200L)
        arrangePage(page = 0, favorites = listOf(first), hasNext = true)
        arrangePage(page = 1, favorites = listOf(second))
        arrangeEstimates(favorites = listOf(first), estimates = listOf(estimate(attractionId = 100L)))
        arrangeEstimates(
            favorites = listOf(second),
            estimates = listOf(estimate(attractionId = 200L, bloomCategory = BloomCategory.PLUM)),
        )
        `when`(deviceTokenRepository.findByUserIdIn(listOf(1L))).thenReturn(emptyList())
        `when`(deviceTokenRepository.findByUserIdIn(listOf(2L))).thenReturn(emptyList())
        `when`(recorder.record(anyCandidate())).thenReturn(notification())

        assertThat(service.sendDueAlerts(TODAY)).isEqualTo(2)

        verify(spotFavoriteRepository).findAlertTargets(
            SpotType.ATTRACTION,
            PageRequest.of(0, props.pageSize),
        )
        verify(spotFavoriteRepository).findAlertTargets(
            SpotType.ATTRACTION,
            PageRequest.of(1, props.pageSize),
        )
        verify(deviceTokenRepository, times(1)).findByUserIdIn(listOf(1L))
        verify(deviceTokenRepository, times(1)).findByUserIdIn(listOf(2L))
    }

    @Test
    fun `오늘과 설정값으로 찜 유형과 추정 조회 정책을 전달한다`() {
        val favorite = favorite()
        arrangePage(page = 0, favorites = listOf(favorite))
        arrangeEstimates(favorites = listOf(favorite), estimates = listOf(estimate()))
        `when`(deviceTokenRepository.findByUserIdIn(listOf(1L))).thenReturn(emptyList())
        `when`(recorder.record(anyCandidate())).thenReturn(notification())
        val spotTypeCaptor = ArgumentCaptor.forClass(SpotType::class.java)
        val pageableCaptor = ArgumentCaptor.forClass(Pageable::class.java)
        val baseDateCaptor = ArgumentCaptor.forClass(LocalDate::class.java)
        val attractionIdsCaptor = collectionCaptor()
        val bloomStatusCaptor = ArgumentCaptor.forClass(BloomStatus::class.java)
        val windowStartCaptor = ArgumentCaptor.forClass(LocalDate::class.java)
        val windowEndCaptor = ArgumentCaptor.forClass(LocalDate::class.java)

        service.sendDueAlerts(TODAY)

        verify(spotFavoriteRepository).findAlertTargets(
            captureSpotType(spotTypeCaptor),
            capturePageable(pageableCaptor),
        )
        verify(seasonalBloomEstimateRepository)
            .findByBaseDateAndAttractionIdInAndStatusNotAndPeakStartDateBetween(
                captureDate(baseDateCaptor),
                captureCollection(attractionIdsCaptor),
                captureBloomStatus(bloomStatusCaptor),
                captureDate(windowStartCaptor),
                captureDate(windowEndCaptor),
            )
        assertThat(spotTypeCaptor.value).isEqualTo(SpotType.ATTRACTION)
        assertThat(pageableCaptor.value).isEqualTo(PageRequest.of(0, props.pageSize))
        assertThat(baseDateCaptor.value).isEqualTo(BASE_DATE)
        assertThat(attractionIdsCaptor.value).containsExactly(100L)
        assertThat(bloomStatusCaptor.value).isEqualTo(BloomStatus.ENDED)
        assertThat(windowStartCaptor.value).isEqualTo(TODAY.plusDays(1))
        assertThat(windowEndCaptor.value).isEqualTo(TODAY.plusDays(props.leadDays))
    }

    private fun arrangePage(
        page: Int,
        favorites: List<AlertTargetFavorite>,
        hasNext: Boolean = false,
    ) {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(BASE_DATE)
        val pageable = PageRequest.of(page, props.pageSize)
        `when`(
            spotFavoriteRepository.findAlertTargets(SpotType.ATTRACTION, pageable),
        ).thenReturn(SliceImpl(favorites, pageable, hasNext))
    }

    private fun arrangeEstimates(
        favorites: List<AlertTargetFavorite>,
        estimates: List<SeasonalBloomEstimate>,
    ) {
        `when`(
            seasonalBloomEstimateRepository
                .findByBaseDateAndAttractionIdInAndStatusNotAndPeakStartDateBetween(
                    BASE_DATE,
                    favorites.map { it.attractionId }.distinct(),
                    BloomStatus.ENDED,
                    TODAY.plusDays(1),
                    TODAY.plusDays(props.leadDays),
                ),
        ).thenReturn(estimates)
    }

    private fun favorite(
        userId: Long = 1L,
        spotId: Long = 10L,
        attractionId: Long = 100L,
    ): AlertTargetFavorite = TestAlertTargetFavorite(
        userId = userId,
        spotId = spotId,
        spotName = "테스트 명소",
        attractionId = attractionId,
    )

    private fun estimate(
        attractionId: Long = 100L,
        bloomCategory: BloomCategory = BloomCategory.CHERRY,
        peakStartDate: LocalDate? = TODAY.plusDays(3),
    ) = SeasonalBloomEstimate(
        attractionId = attractionId,
        bloomCategory = bloomCategory,
        baseDate = BASE_DATE,
        status = BloomStatus.PREPARING,
        confidence = 0.8,
        chosenEstimator = Estimator.CALENDAR,
        peakStartDate = peakStartDate,
    )

    private fun notification(): Notification =
        Notification(
            recipientId = 1L,
            type = NotificationType.TIMING,
            title = BloomTimingAlertMessage.TITLE,
            body = "body",
            linkType = NotificationLinkType.INTERNAL,
            targetId = 10L,
        )

    /** ArgumentCaptor.capture() 도 null 을 돌려주므로 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun captureCandidate(): BloomTimingAlertCandidate = candidateCaptor.capture() ?: DUMMY_CANDIDATE

    /** Mockito.any() 의 null 반환을 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun anyCandidate(): BloomTimingAlertCandidate =
        any(BloomTimingAlertCandidate::class.java) ?: DUMMY_CANDIDATE

    /** Mockito.any() 의 null 반환을 non-null 더미로 감싼다 (매처는 그대로 등록됨). */
    private fun anyPayload(): PushPayload = any(PushPayload::class.java) ?: DUMMY_PAYLOAD

    @Suppress("UNCHECKED_CAST")
    private fun anyTokenList(): List<DeviceToken> =
        any(List::class.java) as? List<DeviceToken> ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun collectionCaptor(): ArgumentCaptor<Collection<Long>> =
        ArgumentCaptor.forClass(Collection::class.java) as ArgumentCaptor<Collection<Long>>

    private fun captureDate(captor: ArgumentCaptor<LocalDate>): LocalDate = captor.capture() ?: LocalDate.MIN

    private fun captureCollection(captor: ArgumentCaptor<Collection<Long>>): Collection<Long> =
        captor.capture() ?: emptyList()

    private fun captureSpotType(captor: ArgumentCaptor<SpotType>): SpotType =
        captor.capture() ?: SpotType.ATTRACTION

    private fun captureBloomStatus(captor: ArgumentCaptor<BloomStatus>): BloomStatus =
        captor.capture() ?: BloomStatus.ENDED

    private fun capturePageable(captor: ArgumentCaptor<Pageable>): Pageable =
        captor.capture() ?: Pageable.unpaged()

    private data class TestAlertTargetFavorite(
        override val userId: Long,
        override val spotId: Long,
        override val spotName: String,
        override val attractionId: Long,
    ) : AlertTargetFavorite

    companion object {
        private val TODAY = LocalDate.of(2026, 4, 1)
        private val BASE_DATE = LocalDate.of(2026, 3, 31)
        private val DUMMY_CANDIDATE = BloomTimingAlertCandidate(
            userId = 0L,
            spotId = 0L,
            spotName = "",
            bloomCategory = BloomCategory.CHERRY,
            peakStartDate = LocalDate.MIN,
            daysUntilPeak = 0,
        )
        private val DUMMY_PAYLOAD = PushPayload(
            title = "",
            body = "",
            linkType = NotificationLinkType.INTERNAL,
            linkUrl = null,
            targetId = null,
        )
    }
}
