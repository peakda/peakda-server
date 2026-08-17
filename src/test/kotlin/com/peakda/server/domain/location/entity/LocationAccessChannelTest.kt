package com.peakda.server.domain.location.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LocationAccessChannelTest {

    @Test
    fun `안드로이드 UA 는 ANDROID 로 판별한다`() {
        val userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S911N) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile"

        assertThat(LocationAccessChannel.from(userAgent)).isEqualTo(LocationAccessChannel.ANDROID)
    }

    @Test
    fun `iOS UA 는 IOS 로 판별한다`() {
        val userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148"

        assertThat(LocationAccessChannel.from(userAgent)).isEqualTo(LocationAccessChannel.IOS)
    }

    @Test
    fun `앱 네트워크 스택 UA 도 IOS 로 판별한다`() {
        assertThat(LocationAccessChannel.from("PEAKDA/1.2.0 CFNetwork/1494.0.7 Darwin/23.4.0"))
            .isEqualTo(LocationAccessChannel.IOS)
    }

    @Test
    fun `안드로이드 표기가 있으면 웹 브라우저 표기보다 우선한다`() {
        val userAgent = "Mozilla/5.0 (Linux; Android 13) Chrome/119.0.0.0 Safari/537.36"

        assertThat(LocationAccessChannel.from(userAgent)).isEqualTo(LocationAccessChannel.ANDROID)
    }

    @Test
    fun `데스크톱 브라우저 UA 는 WEB 으로 판별한다`() {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"

        assertThat(LocationAccessChannel.from(userAgent)).isEqualTo(LocationAccessChannel.WEB)
    }

    @Test
    fun `UA 가 없거나 비어 있으면 UNKNOWN 으로 남긴다`() {
        assertThat(LocationAccessChannel.from(null)).isEqualTo(LocationAccessChannel.UNKNOWN)
        assertThat(LocationAccessChannel.from("   ")).isEqualTo(LocationAccessChannel.UNKNOWN)
    }

    @Test
    fun `판별할 수 없는 UA 는 UNKNOWN 으로 남긴다`() {
        assertThat(LocationAccessChannel.from("curl/8.4.0")).isEqualTo(LocationAccessChannel.UNKNOWN)
    }
}
