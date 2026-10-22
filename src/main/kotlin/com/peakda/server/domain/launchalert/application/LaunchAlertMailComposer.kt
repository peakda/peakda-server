package com.peakda.server.domain.launchalert.application

import com.peakda.server.infrastructure.mail.EmailMessage
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.nio.charset.StandardCharsets

/**
 * 출시 안내 메일을 만든다. HTML 레이아웃(`resources/mail/launch-alert-layout.html`)은 고정이고,
 * 백오피스에서 고친 문구는 모두 이스케이프해 끼워 넣는다. 문구에 HTML 을 쓸 수 없게 해 메일 깨짐과 주입을 막는다.
 */
@Component
class LaunchAlertMailComposer {

    private val layout: String =
        ClassPathResource(LAYOUT_PATH).inputStream.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }

    fun compose(content: LaunchAlertMailContent, storeUrl: String, to: String): EmailMessage =
        EmailMessage(
            to = to,
            subject = content.subject,
            text = text(content, storeUrl),
            html = html(content, storeUrl),
        )

    /** 백오피스 미리보기용. 실제 메일과 같은 HTML 이다. */
    fun html(content: LaunchAlertMailContent, storeUrl: String): String {
        val values = mapOf(
            "subject" to escape(content.subject),
            "headline" to escape(content.headline),
            "body" to paragraphs(content.body),
            "storeUrl" to escape(storeUrl),
            "buttonLabel" to escape(content.buttonLabel),
            "footer" to escape(content.footer).withLineBreaks(),
        )
        // 한 번에 치환한다. 순서대로 바꾸면 문구 안에 적힌 "{{footer}}" 같은 글자까지 치환된다.
        return PLACEHOLDER.replace(layout) { values[it.groupValues[1]] ?: it.value }
    }

    private fun text(content: LaunchAlertMailContent, storeUrl: String): String =
        buildString {
            appendLine(content.headline)
            appendLine()
            appendLine(content.body.normalizeLineEndings().trim())
            appendLine()
            appendLine("${content.buttonLabel}: $storeUrl")
            appendLine()
            appendLine("---")
            appendLine(content.footer.normalizeLineEndings().trim())
        }

    /** 빈 줄로 문단을 나누고, 문단 안 줄바꿈은 `<br>` 로 살린다. */
    private fun paragraphs(body: String): String =
        body.normalizeLineEndings().trim()
            .split(BLANK_LINE)
            .filter { it.isNotBlank() }
            .joinToString("") { """<p style="margin:12px 0 0;">${escape(it.trim()).withLineBreaks()}</p>""" }

    private fun escape(value: String): String = HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name())

    private fun String.withLineBreaks(): String = normalizeLineEndings().replace("\n", "<br>")

    private fun String.normalizeLineEndings(): String = replace("\r\n", "\n").replace('\r', '\n')

    companion object {
        private const val LAYOUT_PATH = "mail/launch-alert-layout.html"
        private val BLANK_LINE = Regex("\n\\s*\n")
        private val PLACEHOLDER = Regex("""\{\{(\w+)}}""")
    }
}
