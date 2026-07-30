package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.application.GddSnapshot
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GddBloomEstimatorTest {

    private val properties = GddEstimatorProperties(
        enabled = true,
        baseConfidence = 0.7,
        endedConfidence = 0.35,
        defaultStationId = STATION_ID,
        thresholds = mapOf(
            BloomCategory.CHERRY.name to GddEstimatorProperties.GddThreshold(
                tBase = 5.4,
                start = 90.0,
                peak = 110.0,
                end = 130.0,
            ),
        ),
    )

    private val estimator = GddBloomEstimator(properties)
    private val baseDate = LocalDate.of(2026, 3, 30)

    @Test
    fun `누적이 시작 임계치 미만이면 준비 상태다`() {
        assertThat(estimator.estimate(context(accumulated = 89.9))!!.status)
            .isEqualTo(BloomStatus.PREPARING)
    }

    @Test
    fun `누적이 시작 이상 절정 미만이면 시작 상태다`() {
        assertThat(estimator.estimate(context(accumulated = 90.0))!!.status)
            .isEqualTo(BloomStatus.STARTED)
    }

    @Test
    fun `누적이 절정 이상 종료 미만이면 절정 상태다`() {
        assertThat(estimator.estimate(context(accumulated = 110.0))!!.status)
            .isEqualTo(BloomStatus.PEAK)
    }

    @Test
    fun `누적이 종료 이상이면 종료 상태다`() {
        assertThat(estimator.estimate(context(accumulated = 130.0))!!.status)
            .isEqualTo(BloomStatus.ENDED)
    }

    @Test
    fun `종료 상태는 종료 신뢰도를 쓰고 다른 상태는 기본 신뢰도를 쓴다`() {
        val ended = estimator.estimate(context(accumulated = 130.0))
        val peak = estimator.estimate(context(accumulated = 110.0))

        assertThat(ended!!.confidence).isEqualTo(properties.endedConfidence)
        assertThat(peak!!.confidence).isEqualTo(properties.baseConfidence)
    }

    @Test
    fun `비활성화 상태면 null 이다`() {
        val disabled = GddBloomEstimator(properties.copy(enabled = false))

        assertThat(disabled.estimate(context(accumulated = 110.0))).isNull()
    }

    @Test
    fun `컨텍스트에 실측 GDD 가 없으면 null 이다`() {
        assertThat(estimator.estimate(context(gdd = null))).isNull()
    }

    @Test
    fun `비온도의존종이면 null 이다`() {
        assertThat(
            estimator.estimate(
                context(category = BloomCategory.HYDRANGEA, accumulated = 110.0),
            ),
        ).isNull()
    }

    @Test
    fun `카테고리 임계치가 없으면 null 이다`() {
        assertThat(
            estimator.estimate(
                context(category = BloomCategory.MAPLE, accumulated = 110.0),
            ),
        ).isNull()
    }

    @Test
    fun `GDD 비율은 누적을 절정 임계치로 나눈 값이다`() {
        val estimation = estimator.estimate(context(accumulated = 100.0))

        assertThat(estimation).isNotNull
        assertThat(estimation!!.estimator).isEqualTo(Estimator.GDD)
        assertThat(estimation.gddRatio).isEqualTo(100.0 / 110.0, offset(1e-9))
    }

    @Test
    fun `근거에 관측 지점 번호가 담긴다`() {
        val estimation = estimator.estimate(context(accumulated = 100.04))

        assertThat(estimation!!.evidence)
            .isEqualTo("gdd:station=108,acc=100.0,tbase=5.4")
    }

    private fun context(
        category: BloomCategory = BloomCategory.CHERRY,
        accumulated: Double = 100.0,
        gdd: GddSnapshot? = GddSnapshot(STATION_ID, accumulated),
    ) = BloomEstimationContext(
        attraction = attraction(),
        category = category,
        baseDate = baseDate,
        gdd = gdd,
    )

    private fun attraction() = Attraction(
        tourApiContentId = "gdd-test",
        title = "남산",
        latitude = 37.55,
        longitude = 126.98,
    )

    companion object {
        private const val STATION_ID = "108"
    }
}
