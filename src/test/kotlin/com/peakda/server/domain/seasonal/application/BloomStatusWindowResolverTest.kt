package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.seasonal.entity.BloomStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BloomStatusWindowResolverTest {

    private val resolver = BloomStatusWindowResolver(
        BloomStatusWindowProperties(startedWindowDays = 7, earlyWindowDays = 14),
    )

    private val peakStart = LocalDate.of(2026, 4, 5)
    private val peakEnd = LocalDate.of(2026, 4, 12)

    @Test
    fun `절정 구간까지 남은 날로 다섯 단계를 가른다`() {
        assertThat(statusOn(peakStart.minusDays(15))).isEqualTo(BloomStatus.BEFORE_SEASON)
        assertThat(statusOn(peakStart.minusDays(14))).isEqualTo(BloomStatus.PREPARING)
        assertThat(statusOn(peakStart.minusDays(8))).isEqualTo(BloomStatus.PREPARING)
        assertThat(statusOn(peakStart.minusDays(7))).isEqualTo(BloomStatus.STARTED)
        assertThat(statusOn(peakStart.minusDays(1))).isEqualTo(BloomStatus.STARTED)
        assertThat(statusOn(peakStart)).isEqualTo(BloomStatus.PEAK)
        assertThat(statusOn(peakEnd)).isEqualTo(BloomStatus.PEAK)
        assertThat(statusOn(peakEnd.plusDays(1))).isEqualTo(BloomStatus.ENDED)
    }

    @Test
    fun `절정 종료일이 없으면 시작일 하루만 절정으로 본다`() {
        assertThat(resolver.statusOn(peakStart, peakStart, null)).isEqualTo(BloomStatus.PEAK)
        assertThat(resolver.statusOn(peakStart.plusDays(1), peakStart, null)).isEqualTo(BloomStatus.ENDED)
    }

    @Test
    fun `절정 시작일을 모르면 판정하지 않는다`() {
        assertThat(resolver.statusOn(peakStart, null, peakEnd)).isNull()
    }

    @Test
    fun `이르다 창 밖의 PREPARING 은 개화전으로 좁혀진다`() {
        assertThat(resolver.narrow(BloomStatus.PREPARING, peakStart.minusDays(15), peakStart))
            .isEqualTo(BloomStatus.BEFORE_SEASON)
        assertThat(resolver.narrow(BloomStatus.PREPARING, peakStart.minusDays(14), peakStart))
            .isEqualTo(BloomStatus.PREPARING)
    }

    @Test
    fun `PREPARING 이 아닌 상태는 날짜로 덮어쓰지 않는다`() {
        // 기온·관측·축제 신호가 직접 판정한 상태다. 절정 구간과 어긋나도 그 판정을 살린다.
        val farFromPeak = peakStart.minusDays(60)
        assertThat(resolver.narrow(BloomStatus.PEAK, farFromPeak, peakStart)).isEqualTo(BloomStatus.PEAK)
        assertThat(resolver.narrow(BloomStatus.STARTED, farFromPeak, peakStart)).isEqualTo(BloomStatus.STARTED)
        assertThat(resolver.narrow(BloomStatus.ENDED, farFromPeak, peakStart)).isEqualTo(BloomStatus.ENDED)
    }

    @Test
    fun `절정 시작일을 모르면 이르다라고 말할 근거가 없어 개화전이 된다`() {
        assertThat(resolver.narrow(BloomStatus.PREPARING, peakStart, null)).isEqualTo(BloomStatus.BEFORE_SEASON)
    }

    private fun statusOn(date: LocalDate) = resolver.statusOn(date, peakStart, peakEnd)
}
