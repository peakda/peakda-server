package com.peakda.server.domain.festival.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FestivalDatesTest {

    @Test
    fun `하이픈 날짜를 파싱한다`() {
        assertThat(FestivalDates.parse("2026-05-01")).isEqualTo(LocalDate.of(2026, 5, 1))
    }

    @Test
    fun `숫자 날짜를 파싱한다`() {
        assertThat(FestivalDates.parse("20260501")).isEqualTo(LocalDate.of(2026, 5, 1))
    }

    @Test
    fun `마침표 날짜를 파싱한다`() {
        assertThat(FestivalDates.parse("2026.05.01")).isEqualTo(LocalDate.of(2026, 5, 1))
    }

    @Test
    fun `비어 있거나 형식이 잘못된 값은 null이다`() {
        assertThat(FestivalDates.parse(null)).isNull()
        assertThat(FestivalDates.parse("")).isNull()
        assertThat(FestivalDates.parse("2026-05")).isNull()
        assertThat(FestivalDates.parse("abc")).isNull()
    }
}
