package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LaunchAlertMailComposerTest {

    private val composer = LaunchAlertMailComposer()

    @Test
    fun `스토어 주소를 본문에 넣고 HTML 에는 이스케이프해 넣는다`() {
        val message = composer.compose(LaunchAlertPlatform.IOS, "https://apps.apple.com/kr/app/id1?a=1&b=2", "a@peakda.com")

        assertThat(message.to).isEqualTo("a@peakda.com")
        assertThat(message.subject).isNotBlank()
        assertThat(message.text).contains("https://apps.apple.com/kr/app/id1?a=1&b=2").doesNotContain("{{")
        assertThat(message.html).contains("href=\"https://apps.apple.com/kr/app/id1?a=1&amp;b=2\"").doesNotContain("{{")
    }

    @Test
    fun `메일 객체 문자열에는 수신 주소가 드러나지 않는다`() {
        val message = composer.compose(LaunchAlertPlatform.IOS, "https://apps.apple.com/kr/app/id1", "a@peakda.com")

        assertThat(message.toString()).doesNotContain("a@peakda.com")
    }
}
