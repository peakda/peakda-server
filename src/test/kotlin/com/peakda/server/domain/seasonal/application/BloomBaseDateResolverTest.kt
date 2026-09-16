package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class BloomBaseDateResolverTest {

    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)

    @Test
    fun `유효 기간 안의 산출일은 그대로 기준일이 된다`() {
        val baseDate = LocalDate.of(2026, 9, 15)
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)

        assertThat(resolverOn(LocalDate.of(2026, 9, 16)).currentBaseDate()).isEqualTo(baseDate)
    }

    @Test
    fun `산출 잡이 멈춰 오래된 산출일만 남으면 기준일이 없다고 본다`() {
        // 봄에 멈춘 산출이 가을까지 "현재 상태"로 나가면 철 지난 명소가 계속 절정으로 보인다.
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(LocalDate.of(2026, 4, 5))

        assertThat(resolverOn(LocalDate.of(2026, 9, 16)).currentBaseDate()).isNull()
    }

    @Test
    fun `유효 기간 경계일까지는 인정하고 하루만 더 밀리면 버린다`() {
        val baseDate = LocalDate.of(2026, 9, 10)
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)

        val lastValidDay = baseDate.plusDays(BloomBaseDateProperties().maxAgeDays)
        assertThat(resolverOn(lastValidDay).currentBaseDate()).isEqualTo(baseDate)
        assertThat(resolverOn(lastValidDay.plusDays(1)).currentBaseDate()).isNull()
    }

    @Test
    fun `산출이 아예 없으면 null 이다`() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(null)

        assertThat(resolverOn(LocalDate.of(2026, 9, 16)).currentBaseDate()).isNull()
    }

    private fun resolverOn(today: LocalDate) = BloomBaseDateResolver(
        seasonalBloomEstimateRepository,
        BloomBaseDateProperties(),
        Clock.fixed(today.atStartOfDay(KST).toInstant(), KST),
    )

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
