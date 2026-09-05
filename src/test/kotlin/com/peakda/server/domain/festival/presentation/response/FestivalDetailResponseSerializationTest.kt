package com.peakda.server.domain.festival.presentation.response

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FestivalDetailResponseSerializationTest {
    @Test
    fun `dDay는 dDay 하나의 JSON 키로 직렬화된다`() {
        val response = FestivalDetailResponse(
            festivalId = 1L,
            name = "축제",
            venue = "축제장",
            roadAddress = null,
            latitude = null,
            longitude = null,
            homepageUrl = null,
            category = null,
            displayName = null,
            startsOn = null,
            endsOn = null,
            durationDays = null,
            phase = null,
            dDay = 12L,
            endsInDays = null,
            editorial = null,
        )

        val json = jacksonObjectMapper().writeValueAsString(response)

        assertThat(json).contains("\"dDay\":12")
        assertThat(json).doesNotContain("\"dday\"")
    }
}
