package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BloomEstimateOrderingTest {

    @Test
    fun `신뢰도가 높은 추정이 대표가 된다`() {
        // 상태를 먼저 보면 신뢰도 0.3 짜리 절정이 0.9 짜리 개화 전을 이겨, 명소가 근거 없이 만개로 보인다.
        val weakPeak = estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.3)
        val strongPreparing = estimate(BloomCategory.MAPLE, BloomStatus.PREPARING, confidence = 0.9)

        val representative = listOf(weakPeak, strongPreparing).minWith(BloomEstimateOrdering.REPRESENTATIVE_FIRST)

        assertThat(representative).isEqualTo(strongPreparing)
    }

    @Test
    fun `신뢰도가 같으면 더 확정적인 상태를 앞세운다`() {
        val preparing = estimate(BloomCategory.MAPLE, BloomStatus.PREPARING, confidence = 0.8)
        val peak = estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.8)

        val ordered = listOf(preparing, peak).sortedWith(BloomEstimateOrdering.REPRESENTATIVE_FIRST)

        assertThat(ordered).containsExactly(peak, preparing)
    }

    private fun estimate(category: BloomCategory, status: BloomStatus, confidence: Double) = SeasonalBloomEstimate(
        attractionId = 501L,
        bloomCategory = category,
        baseDate = LocalDate.of(2026, 9, 16),
        status = status,
        confidence = confidence,
        chosenEstimator = Estimator.CALENDAR,
    )
}
