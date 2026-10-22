package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertMailTemplate

/** 메일에 들어갈 문구. 저장된 문구([LaunchAlertMailTemplate])에서 만든다. */
data class LaunchAlertMailContent(
    val subject: String,
    val headline: String,
    val body: String,
    val buttonLabel: String,
    val footer: String,
) {
    companion object {
        fun from(template: LaunchAlertMailTemplate) = LaunchAlertMailContent(
            subject = template.subject,
            headline = template.headline,
            body = template.body,
            buttonLabel = template.buttonLabel,
            footer = template.footer,
        )
    }
}
