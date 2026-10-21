package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.infrastructure.mail.EmailMessage
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.nio.charset.StandardCharsets

/**
 * 출시 안내 메일을 만든다. 문구는 `resources/mail/launch-alert-<platform>.{txt,html}` 에 두고
 * `{{storeUrl}}` 만 바꿔 끼운다. 문구를 고칠 때 코드를 건드리지 않게 하려는 것이다.
 */
@Component
class LaunchAlertMailComposer {

    private val templates: Map<LaunchAlertPlatform, Template> =
        LaunchAlertPlatform.entries.associateWith { platform ->
            val name = "mail/launch-alert-${platform.name.lowercase()}"
            Template(text = read("$name.txt"), html = read("$name.html"))
        }

    fun compose(platform: LaunchAlertPlatform, storeUrl: String, to: String): EmailMessage {
        val template = templates.getValue(platform)
        return EmailMessage(
            to = to,
            subject = SUBJECTS.getValue(platform),
            text = template.text.replace(STORE_URL, storeUrl),
            html = template.html.replace(STORE_URL, HtmlUtils.htmlEscape(storeUrl)),
        )
    }

    private fun read(path: String): String =
        ClassPathResource(path).inputStream.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }

    private data class Template(val text: String, val html: String)

    companion object {
        private const val STORE_URL = "{{storeUrl}}"
        private val SUBJECTS = mapOf(
            LaunchAlertPlatform.IOS to "피크다 iOS 앱이 출시됐어요",
        )
    }
}
