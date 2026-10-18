package com.peakda.server.domain.auth.session.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WebDeviceNamesTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36|Chrome (macOS)",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0|Edge (Windows)",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1|Safari (iOS)",
            "Mozilla/5.0 (Linux; Android 14; SM-S918N) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/26.0 Chrome/122.0.0.0 Mobile Safari/537.36|Samsung Internet (Android)",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Whale/4.33.0.0 Safari/537.36|Whale (Windows)",
        ],
    )
    fun `User-Agent 를 브라우저 (OS) 이름으로 만든다`(userAgent: String, expected: String) {
        assertThat(WebDeviceNames.of(userAgent)).isEqualTo(expected)
    }

    @org.junit.jupiter.api.Test
    fun `User-Agent 가 없으면 웹 브라우저로 표시한다`() {
        assertThat(WebDeviceNames.of(null)).isEqualTo("웹 브라우저")
    }
}
