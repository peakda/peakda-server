package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.application.ObservationSnapshot
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ObservationBloomEstimatorTest {

    private val properties = ObservationEstimatorProperties(
        enabled = true,
        baseConfidence = 0.9,
        peakDurationDays = 7,
    )
    private val estimator = ObservationBloomEstimator(properties)

    @Test
    fun `개화일 전이면 준비 상태다`() {
        val estimation = estimator.estimate(
            context(
                baseDate = LocalDate.of(2026, 3, 24),
                floweringOn = LocalDate.of(2026, 3, 25),
                fullBloomOn = LocalDate.of(2026, 3, 30),
            ),
        )

        assertThat(estimation!!.status).isEqualTo(BloomStatus.PREPARING)
    }

    @Test
    fun `개화일 이후 만발일 전이면 시작 상태다`() {
        val estimation = estimator.estimate(
            context(
                baseDate = LocalDate.of(2026, 3, 27),
                floweringOn = LocalDate.of(2026, 3, 25),
                fullBloomOn = LocalDate.of(2026, 3, 30),
            ),
        )

        assertThat(estimation!!.status).isEqualTo(BloomStatus.STARTED)
    }

    @Test
    fun `만발일부터 지속일수까지는 절정 상태다`() {
        val estimation = estimator.estimate(
            context(
                baseDate = LocalDate.of(2026, 4, 6),
                floweringOn = LocalDate.of(2026, 3, 25),
                fullBloomOn = LocalDate.of(2026, 3, 30),
            ),
        )

        assertThat(estimation!!.status).isEqualTo(BloomStatus.PEAK)
    }

    @Test
    fun `지속일수를 넘기면 종료 상태다`() {
        val estimation = estimator.estimate(
            context(
                baseDate = LocalDate.of(2026, 4, 7),
                floweringOn = LocalDate.of(2026, 3, 25),
                fullBloomOn = LocalDate.of(2026, 3, 30),
            ),
        )

        assertThat(estimation!!.status).isEqualTo(BloomStatus.ENDED)
    }

    @Test
    fun `만발일이 없으면 개화일만으로 시작 상태를 낸다`() {
        val estimation = estimator.estimate(
            context(
                baseDate = LocalDate.of(2026, 3, 27),
                floweringOn = LocalDate.of(2026, 3, 25),
                fullBloomOn = null,
            ),
        )

        assertThat(estimation!!.status).isEqualTo(BloomStatus.STARTED)
        assertThat(estimation.peakStartDate).isNull()
        assertThat(estimation.peakEndDate).isNull()
    }

    @Test
    fun `개화일이 없으면 관측 전으로 보고 null 이다`() {
        assertThat(
            estimator.estimate(
                context(
                    baseDate = LocalDate.of(2026, 3, 27),
                    floweringOn = null,
                    fullBloomOn = null,
                ),
            ),
        ).isNull()
    }

    @Test
    fun `비활성화 상태면 null 이다`() {
        val disabled = ObservationBloomEstimator(properties.copy(enabled = false))

        assertThat(disabled.estimate(context())).isNull()
    }

    @Test
    fun `peakStartDate 는 만발일로 채운다`() {
        val fullBloomOn = LocalDate.of(2026, 3, 30)
        val estimation = estimator.estimate(
            context(
                floweringOn = LocalDate.of(2026, 3, 25),
                fullBloomOn = fullBloomOn,
            ),
        )

        assertThat(estimation!!.estimator).isEqualTo(Estimator.OBSERVATION)
        assertThat(estimation.peakStartDate).isEqualTo(fullBloomOn)
        assertThat(estimation.peakEndDate).isEqualTo(fullBloomOn.plusDays(properties.peakDurationDays))
    }

    private fun context(
        baseDate: LocalDate = LocalDate.of(2026, 3, 30),
        floweringOn: LocalDate? = LocalDate.of(2026, 3, 25),
        fullBloomOn: LocalDate? = LocalDate.of(2026, 3, 30),
    ) = BloomEstimationContext(
        attraction = Attraction(
            tourApiContentId = "observation-test",
            title = "여의도",
            latitude = 37.52,
            longitude = 126.92,
        ),
        category = BloomCategory.CHERRY,
        baseDate = baseDate,
        observation = ObservationSnapshot(
            obsPlace = "여의도 윤중로",
            floweringOn = floweringOn,
            fullBloomOn = fullBloomOn,
        ),
    )
}
