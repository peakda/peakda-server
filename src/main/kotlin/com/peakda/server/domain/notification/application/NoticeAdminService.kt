package com.peakda.server.domain.notification.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.notification.entity.Notice
import com.peakda.server.domain.notification.entity.NoticeStatus
import com.peakda.server.domain.notification.exception.NoticeAlreadyDispatchedException
import com.peakda.server.domain.notification.exception.NoticeNotEditableException
import com.peakda.server.domain.notification.exception.NoticeNotFoundException
import com.peakda.server.domain.notification.presentation.response.NoticeResponse
import com.peakda.server.domain.notification.repository.NoticeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NoticeAdminService(
    private val noticeRepository: NoticeRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
) {

    fun create(adminId: Long, command: UpsertNoticeCommand): NoticeResponse {
        val notice = noticeRepository.saveAndFlush(
            Notice(
                title = command.title.trim(),
                body = command.body.trim(),
                linkType = command.linkType,
                linkUrl = command.linkUrl.normalized(),
                targetId = command.targetId,
                createdBy = adminId,
            ),
        )
        record(adminId, AdminAuditAction.NOTICE_CREATE, requireNotNull(notice.id))
        return NoticeResponse.from(notice)
    }

    @Transactional(readOnly = true)
    fun list(status: NoticeStatus?, pageable: Pageable): Page<NoticeResponse> =
        if (status == null) {
            noticeRepository.findAllByOrderByIdDesc(pageable)
        } else {
            noticeRepository.findByStatusOrderByIdDesc(status, pageable)
        }.map(NoticeResponse::from)

    @Transactional(readOnly = true)
    fun get(id: Long): NoticeResponse =
        NoticeResponse.from(noticeRepository.findById(id).orElseThrow { NoticeNotFoundException() })

    fun update(adminId: Long, id: Long, command: UpsertNoticeCommand): NoticeResponse {
        val notice = findForUpdate(id)
        if (notice.status != NoticeStatus.DRAFT) throw NoticeNotEditableException()

        notice.update(
            title = command.title.trim(),
            body = command.body.trim(),
            linkType = command.linkType,
            linkUrl = command.linkUrl.normalized(),
            targetId = command.targetId,
        )
        record(adminId, AdminAuditAction.NOTICE_UPDATE, id)
        noticeRepository.flush()
        return NoticeResponse.from(notice)
    }

    fun dispatch(adminId: Long, id: Long): NoticeResponse {
        val notice = findForUpdate(id)
        if (notice.status != NoticeStatus.DRAFT) throw NoticeAlreadyDispatchedException()

        notice.startDispatch()
        record(adminId, AdminAuditAction.NOTICE_DISPATCH, id)
        noticeRepository.flush()
        return NoticeResponse.from(notice)
    }

    fun cancel(adminId: Long, id: Long): NoticeResponse {
        val notice = findForUpdate(id)
        if (notice.status != NoticeStatus.DRAFT) throw NoticeNotEditableException()

        notice.cancel()
        record(adminId, AdminAuditAction.NOTICE_CANCEL, id)
        noticeRepository.flush()
        return NoticeResponse.from(notice)
    }

    private fun findForUpdate(id: Long): Notice =
        noticeRepository.findByIdForUpdate(id) ?: throw NoticeNotFoundException()

    private fun record(adminId: Long, action: AdminAuditAction, noticeId: Long) {
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = action,
                targetType = AdminAuditTargetType.NOTICE,
                targetId = noticeId,
            ),
        )
    }

    private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}
