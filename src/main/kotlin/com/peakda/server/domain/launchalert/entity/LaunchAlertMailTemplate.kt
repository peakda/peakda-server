package com.peakda.server.domain.launchalert.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 출시 안내 메일 문구. 플랫폼마다 한 행이고 기본 문구는 마이그레이션이 넣는다.
 * HTML 레이아웃은 서버에 고정돼 있고 여기 문구만 이스케이프해 끼워 넣는다.
 */
@Entity
@Table(name = "launch_alert_mail_templates")
class LaunchAlertMailTemplate(
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", columnDefinition = "TEXT")
    val platform: LaunchAlertPlatform,

    subject: String,
    headline: String,
    body: String,
    buttonLabel: String,
    footer: String,
) : BaseTimeEntity() {

    @Column(name = "subject", nullable = false, columnDefinition = "TEXT")
    var subject: String = subject
        protected set

    @Column(name = "headline", nullable = false, columnDefinition = "TEXT")
    var headline: String = headline
        protected set

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    var body: String = body
        protected set

    @Column(name = "button_label", nullable = false, columnDefinition = "TEXT")
    var buttonLabel: String = buttonLabel
        protected set

    @Column(name = "footer", nullable = false, columnDefinition = "TEXT")
    var footer: String = footer
        protected set

    @Column(name = "updated_by")
    var updatedBy: Long? = null
        protected set

    fun update(subject: String, headline: String, body: String, buttonLabel: String, footer: String, adminId: Long) {
        this.subject = subject
        this.headline = headline
        this.body = body
        this.buttonLabel = buttonLabel
        this.footer = footer
        updatedBy = adminId
    }
}
