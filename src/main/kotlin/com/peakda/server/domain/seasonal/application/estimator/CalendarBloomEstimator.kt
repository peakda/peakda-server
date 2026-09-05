package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.seasonal.application.BloomEstimation
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.MonthDayRange
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 신호 C — 평년 절정 구간(`BloomCategory.typicalPeakRange`) 기반 달력 추정기. 항상 가용한 fallback 으로 낮은 신뢰도를 갖는다.
 *
 * baseDate 인근 시즌의 절정 구간을 실제 날짜로 환산하고, early/late 윈도우로 STARTED/PEAK/ENDED/PREPARING 을 가린다.
 * 동백처럼 연말을 넘는 구간도 다년 후보 윈도우로 처리한다.
 */
@Component
class CalendarBloomEstimator(
    private val properties: CalendarEstimatorProperties,
) : BloomEstimator {

    override val estimator = Estimator.CALENDAR

    override fun estimate(context: BloomEstimationContext): BloomEstimation {
        val baseDate = context.baseDate
        val windows = peakWindowsAround(context.category.typicalPeakRange, baseDate)

        for (window in windows) {
            val startedFrom = window.start.minusDays(properties.earlyWindowDays)
            val endedTo = window.end.plusDays(properties.lateWindowDays)
            val status = when {
                !baseDate.isBefore(window.start) && !baseDate.isAfter(window.end) -> BloomStatus.PEAK
                !baseDate.isBefore(startedFrom) && baseDate.isBefore(window.start) -> BloomStatus.STARTED
                baseDate.isAfter(window.end) && !baseDate.isAfter(endedTo) -> BloomStatus.ENDED
                else -> null
            } ?: continue
            return estimation(status, window)
        }

        // 어느 윈도우에도 걸치지 않으면 다가오는 가장 가까운 절정 구간을 PREPARING 으로 안내한다.
        val upcoming = windows.firstOrNull { it.start.isAfter(baseDate) } ?: windows.last()
        return estimation(BloomStatus.PREPARING, upcoming)
    }

    private fun estimation(status: BloomStatus, window: PeakWindow) = BloomEstimation(
        estimator = Estimator.CALENDAR,
        status = status,
        confidence = properties.baseConfidence,
        peakStartDate = window.start,
        peakEndDate = window.end,
        evidence = "calendar:${window.start}~${window.end}",
    )

    /** typicalPeakRange 를 baseDate 전후 3개 시즌의 실제 날짜 구간으로 환산한다 (시작일 오름차순). */
    private fun peakWindowsAround(range: MonthDayRange, baseDate: LocalDate): List<PeakWindow> =
        listOf(baseDate.year - 1, baseDate.year, baseDate.year + 1)
            .map { year ->
                val start = range.from.atYear(year)
                val end = if (range.from <= range.to) range.to.atYear(year) else range.to.atYear(year + 1)
                PeakWindow(start, end)
            }
            .sortedBy { it.start }

    private data class PeakWindow(val start: LocalDate, val end: LocalDate)
}
