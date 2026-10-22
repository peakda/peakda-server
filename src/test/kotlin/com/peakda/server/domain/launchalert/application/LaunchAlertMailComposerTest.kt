package com.peakda.server.domain.launchalert.application

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LaunchAlertMailComposerTest {

    private val composer = LaunchAlertMailComposer()

    @Test
    fun `문구는 이스케이프하고 본문은 빈 줄로 문단을 나누며 줄바꿈을 살린다`() {
        val message = composer.compose(
            content(headline = "<b>출시</b>", body = "첫 문단\n둘째 줄\n\n둘째 문단"),
            "https://apps.apple.com/kr/app/id1?a=1&b=2",
            "a@peakda.com",
        )

        assertThat(message.html)
            .contains("&lt;b&gt;출시&lt;/b&gt;")
            .contains(">첫 문단<br>둘째 줄</p>")
            .contains(">둘째 문단</p>")
            .contains("href=\"https://apps.apple.com/kr/app/id1?a=1&amp;b=2\"")
            .doesNotContain("{{")
        assertThat(message.text)
            .contains("<b>출시</b>")
            .contains("App Store에서 받기: https://apps.apple.com/kr/app/id1?a=1&b=2")
        assertThat(message.subject).isEqualTo("제목")
    }

    @Test
    fun `문구 안의 자리표시자 글자는 치환하지 않는다`() {
        val html = composer.html(content(headline = "{{footer}}", footer = "하단"), "https://apps.apple.com/kr/app/id1")

        assertThat(html).contains(">{{footer}}</td>")
    }

    @Test
    fun `메일 객체 문자열에는 수신 주소가 드러나지 않는다`() {
        val message = composer.compose(content(), "https://apps.apple.com/kr/app/id1", "a@peakda.com")

        assertThat(message.toString()).doesNotContain("a@peakda.com")
    }

    private fun content(
        headline: String = "큰 제목",
        body: String = "본문",
        footer: String = "하단 안내",
    ) = LaunchAlertMailContent(
        subject = "제목",
        headline = headline,
        body = body,
        buttonLabel = "App Store에서 받기",
        footer = footer,
    )
}
