package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.application.estimator.BloomEstimator
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BloomStatusFusionServiceTest {

    private val properties = BloomFusionProperties(
        agreementBonus = 0.1,
        agreementBonusCap = 0.9,
        tieBreakConfidenceMargin = 0.1,
    )

    @Test
    fun `모든 추정기가 null 이면 null 을 돌려준다`() {
        val service = service(
            stub(Estimator.GDD, null),
            stub(Estimator.CALENDAR, null),
        )

        assertThat(service.fuse(context())).isNull()
    }

    @Test
    fun `신뢰도가 가장 높은 추정이 채택된다`() {
        val service = service(
            stub(Estimator.GDD, estimation(Estimator.GDD, BloomStatus.PEAK, 0.8)),
            stub(Estimator.CALENDAR, estimation(Estimator.CALENDAR, BloomStatus.PREPARING, 0.5)),
        )

        val result = service.fuse(context())

        assertThat(result!!.estimator).isEqualTo(Estimator.GDD)
        assertThat(result.status).isEqualTo(BloomStatus.PEAK)
        assertThat(result.confidence).isEqualTo(0.8)
    }

    @Test
    fun `상위 두 추정의 상태가 다르고 신뢰도 차가 기준 미만이면 보수적 상태를 채택한다`() {
        val service = service(
            stub(Estimator.GDD, estimation(Estimator.GDD, BloomStatus.PEAK, 0.7)),
            stub(Estimator.CALENDAR, estimation(Estimator.CALENDAR, BloomStatus.STARTED, 0.65)),
        )

        val result = service.fuse(context())

        assertThat(result!!.estimator).isEqualTo(Estimator.CALENDAR)
        assertThat(result.status).isEqualTo(BloomStatus.STARTED)
        assertThat(result.confidence).isEqualTo(0.65)
    }

    @Test
    fun `같은 상태에 동의하는 추정기가 여럿이면 신뢰도가 가산되고 상한을 넘지 않는다`() {
        val service = service(
            stub(Estimator.GDD, estimation(Estimator.GDD, BloomStatus.PEAK, 0.8)),
            stub(Estimator.FESTIVAL, estimation(Estimator.FESTIVAL, BloomStatus.PEAK, 0.7)),
            stub(Estimator.CALENDAR, estimation(Estimator.CALENDAR, BloomStatus.PEAK, 0.6)),
        )

        val result = service.fuse(context())

        assertThat(result!!.confidence).isEqualTo(properties.agreementBonusCap)
    }

    @Test
    fun `채택된 추정에 절정 구간이 없으면 다른 추정의 구간을 승계한다`() {
        val calendarStart = LocalDate.of(2026, 4, 1)
        val calendarEnd = LocalDate.of(2026, 4, 7)
        val service = service(
            stub(Estimator.GDD, estimation(Estimator.GDD, BloomStatus.PEAK, 0.7)),
            stub(
                Estimator.CALENDAR,
                estimation(
                    Estimator.CALENDAR,
                    BloomStatus.PREPARING,
                    0.4,
                    calendarStart,
                    calendarEnd,
                ),
            ),
        )

        val result = service.fuse(context())

        assertThat(result!!.estimator).isEqualTo(Estimator.GDD)
        assertThat(result.peakStartDate).isEqualTo(calendarStart)
        assertThat(result.peakEndDate).isEqualTo(calendarEnd)
        assertThat(result.peakDurationDays).isEqualTo(7)
    }

    @Test
    fun `채택된 추정에 절정 구간이 이미 있으면 승계하지 않는다`() {
        val festivalStart = LocalDate.of(2026, 3, 28)
        val festivalEnd = LocalDate.of(2026, 4, 5)
        val service = service(
            stub(
                Estimator.FESTIVAL,
                estimation(
                    Estimator.FESTIVAL,
                    BloomStatus.PEAK,
                    0.9,
                    festivalStart,
                    festivalEnd,
                ),
            ),
            stub(
                Estimator.CALENDAR,
                estimation(
                    Estimator.CALENDAR,
                    BloomStatus.PREPARING,
                    0.4,
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 7),
                ),
            ),
        )

        val result = service.fuse(context())

        assertThat(result!!.estimator).isEqualTo(Estimator.FESTIVAL)
        assertThat(result.peakStartDate).isEqualTo(festivalStart)
        assertThat(result.peakEndDate).isEqualTo(festivalEnd)
    }

    @Test
    fun `모든 추정에 절정 구간이 없으면 null 로 남는다`() {
        val service = service(
            stub(Estimator.GDD, estimation(Estimator.GDD, BloomStatus.PEAK, 0.7)),
            stub(Estimator.USER_RECORD, estimation(Estimator.USER_RECORD, BloomStatus.PEAK, 0.6)),
        )

        val result = service.fuse(context())

        assertThat(result!!.peakStartDate).isNull()
        assertThat(result.peakEndDate).isNull()
        assertThat(result.peakDurationDays).isNull()
    }

    @Test
    fun `승계 시 신뢰도 내림차순으로 앞선 추정의 구간을 쓴다`() {
        val festivalStart = LocalDate.of(2026, 3, 28)
        val festivalEnd = LocalDate.of(2026, 4, 5)
        val service = service(
            stub(Estimator.GDD, estimation(Estimator.GDD, BloomStatus.PEAK, 0.85)),
            stub(
                Estimator.CALENDAR,
                estimation(
                    Estimator.CALENDAR,
                    BloomStatus.PEAK,
                    0.4,
                    LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 4, 7),
                ),
            ),
            stub(
                Estimator.FESTIVAL,
                estimation(
                    Estimator.FESTIVAL,
                    BloomStatus.PEAK,
                    0.8,
                    festivalStart,
                    festivalEnd,
                ),
            ),
        )

        val result = service.fuse(context())

        assertThat(result!!.estimator).isEqualTo(Estimator.GDD)
        assertThat(result.peakStartDate).isEqualTo(festivalStart)
        assertThat(result.peakEndDate).isEqualTo(festivalEnd)
    }

    @Test
    fun `관측과 축제가 같은 신뢰도면 관측이 채택된다`() {
        val service = service(
            stub(Estimator.FESTIVAL, estimation(Estimator.FESTIVAL, BloomStatus.PEAK, 0.9)),
            stub(Estimator.OBSERVATION, estimation(Estimator.OBSERVATION, BloomStatus.PEAK, 0.9)),
        )

        val result = service.fuse(context())

        assertThat(result!!.estimator).isEqualTo(Estimator.OBSERVATION)
        assertThat(result.status).isEqualTo(BloomStatus.PEAK)
    }

    private fun service(vararg estimators: BloomEstimator) =
        BloomStatusFusionService(estimators.toList(), properties)

    private fun stub(estimator: Estimator, result: BloomEstimation?) = StubEstimator(estimator, result)

    private fun estimation(
        estimator: Estimator,
        status: BloomStatus,
        confidence: Double,
        peakStartDate: LocalDate? = null,
        peakEndDate: LocalDate? = null,
    ) = BloomEstimation(
        estimator = estimator,
        status = status,
        confidence = confidence,
        peakStartDate = peakStartDate,
        peakEndDate = peakEndDate,
    )

    private fun context() = BloomEstimationContext(
        attraction = attraction(),
        category = BloomCategory.CHERRY,
        baseDate = LocalDate.of(2026, 3, 30),
    )

    private fun attraction() = Attraction(
        tourApiContentId = "fusion-test",
        title = "남산",
        latitude = 37.55,
        longitude = 126.98,
    )

    private class StubEstimator(
        override val estimator: Estimator,
        private val result: BloomEstimation?,
    ) : BloomEstimator {
        override fun estimate(context: BloomEstimationContext) = result
    }
}
