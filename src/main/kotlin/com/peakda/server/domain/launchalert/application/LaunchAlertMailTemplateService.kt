package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.entity.LaunchAlertMailTemplate
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.exception.LaunchAlertDispatchInProgressException
import com.peakda.server.domain.launchalert.exception.LaunchAlertMailTemplateNotFoundException
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertMailTemplateResponse
import com.peakda.server.domain.launchalert.repository.LaunchAlertDispatchRepository
import com.peakda.server.domain.launchalert.repository.LaunchAlertMailTemplateRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 출시 안내 메일 문구 조회·수정·미리보기. */
@Service
@Transactional(readOnly = true)
class LaunchAlertMailTemplateService(
    private val templateRepository: LaunchAlertMailTemplateRepository,
    private val dispatchRepository: LaunchAlertDispatchRepository,
    private val mailComposer: LaunchAlertMailComposer,
    private val adminAuditRecorder: AdminAuditRecorder,
) {

    fun get(platform: LaunchAlertPlatform): LaunchAlertMailTemplateResponse =
        LaunchAlertMailTemplateResponse.from(find(platform))

    /** 발송과 테스트 발송이 쓰는 현재 문구 */
    fun content(platform: LaunchAlertPlatform): LaunchAlertMailContent = LaunchAlertMailContent.from(find(platform))

    /** 저장된 문구로 만든 실제 메일 HTML. 스토어 주소가 없거나 규칙에 안 맞으면 기본 주소를 건다. */
    fun previewHtml(platform: LaunchAlertPlatform, storeUrl: String?): String {
        val url = storeUrl?.trim()?.takeIf(LaunchAlertStoreUrl::isValid) ?: PREVIEW_STORE_URL
        return mailComposer.html(content(platform), url)
    }

    /**
     * 문구를 고친다. 보내는 중에 바꾸면 앞뒤 수신자가 다른 메일을 받으므로 막는다.
     * 문구 행을 잠근 뒤 발송 중인지 본다. 발송 요청도 같은 행을 잠그므로 둘이 동시에 지나가지 못한다.
     * 문구는 개인정보가 아니라 감사 로그에는 남기지 않고 수정 사실만 남긴다.
     */
    @Transactional
    fun update(adminId: Long, command: UpdateLaunchAlertMailTemplateCommand): LaunchAlertMailTemplateResponse {
        val template = templateRepository.findByIdForUpdate(command.platform)
            ?: throw LaunchAlertMailTemplateNotFoundException()
        if (dispatchRepository.existsByPlatformAndStatus(command.platform, LaunchAlertDispatchStatus.DISPATCHING)) {
            throw LaunchAlertDispatchInProgressException()
        }
        with(command.content) {
            template.update(
                subject = subject.trim(),
                headline = headline.trim(),
                body = body.trim(),
                buttonLabel = buttonLabel.trim(),
                footer = footer.trim(),
                adminId = adminId,
            )
        }
        templateRepository.flush()
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = AdminAuditAction.LAUNCH_ALERT_MAIL_TEMPLATE_UPDATE,
                targetType = AdminAuditTargetType.LAUNCH_ALERT,
                targetId = 0,
                memo = command.platform.name,
            ),
        )
        return LaunchAlertMailTemplateResponse.from(template)
    }

    private fun find(platform: LaunchAlertPlatform): LaunchAlertMailTemplate =
        templateRepository.findById(platform).orElseThrow { LaunchAlertMailTemplateNotFoundException() }

    companion object {
        /** 스토어 주소를 아직 모를 때 미리보기 버튼에 거는 주소 */
        private const val PREVIEW_STORE_URL = "https://apps.apple.com/"
    }
}
