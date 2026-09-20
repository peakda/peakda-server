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

    @Test
    fun `이미 진 추정은 신뢰도가 높아도 시즌이 남은 추정보다 뒤에 온다`() {
        // 관측이 붙은 벚꽃의 늦었다(0.9)가 달력뿐인 단풍의 절정(0.4)을 이기면 가을 내내 늦었다가 대표가 된다.
        val endedCherry = estimate(BloomCategory.CHERRY, BloomStatus.ENDED, confidence = 0.9)
        val peakMaple = estimate(BloomCategory.MAPLE, BloomStatus.PEAK, confidence = 0.4)

        val ordered = listOf(endedCherry, peakMaple).sortedWith(BloomEstimateOrdering.REPRESENTATIVE_FIRST)

        assertThat(ordered).containsExactly(peakMaple, endedCherry)
    }

    @Test
    fun `아직 시즌이 아닌 추정은 이미 진 추정보다도 뒤에 온다`() {
        val beforeSeason = estimate(BloomCategory.CHERRY, BloomStatus.BEFORE_SEASON, confidence = 0.4)
        val ended = estimate(BloomCategory.MAPLE, BloomStatus.ENDED, confidence = 0.4)

        val ordered = listOf(beforeSeason, ended).sortedWith(BloomEstimateOrdering.REPRESENTATIVE_FIRST)

        assertThat(ordered).containsExactly(ended, beforeSeason)
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
