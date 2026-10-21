package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatch
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.exception.LaunchAlertDispatchInProgressException
import com.peakda.server.domain.launchalert.exception.LaunchAlertDispatchNotCancelableException
import com.peakda.server.domain.launchalert.exception.LaunchAlertDispatchNotFoundException
import com.peakda.server.domain.launchalert.exception.LaunchAlertMailSendFailedException
import com.peakda.server.domain.launchalert.exception.LaunchAlertMailUnavailableException
import com.peakda.server.domain.launchalert.exception.LaunchAlertNoRecipientsException
import com.peakda.server.domain.launchalert.exception.LaunchAlertSubscriptionNotFoundException
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertDispatchResponse
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertSubscriptionResponse
import com.peakda.server.domain.launchalert.repository.LaunchAlertDispatchRepository
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import com.peakda.server.infrastructure.mail.EmailSendException
import com.peakda.server.infrastructure.mail.EmailSender
import com.peakda.server.infrastructure.scheduler.SchedulerProperties
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * 출시 알림 신청과 출시 안내 발송 관리.
 *
 * 발송은 요청만 만들고 [LaunchAlertFanoutService] 가 스케줄러 잡에서 보낸다. 메일이 꺼진 환경에서 발송을
 * 시작하면 아무에게도 안 보낸 채 신청만 지워지므로, 요청 단계에서 먼저 막는다.
 * 감사 로그 memo 에는 이메일을 남기지 않는다.
 */
@Service
@Transactional
class LaunchAlertAdminService(
    private val subscriptionRepository: LaunchAlertSubscriptionRepository,
    private val dispatchRepository: LaunchAlertDispatchRepository,
    private val mailComposer: LaunchAlertMailComposer,
    private val emailSender: EmailSender,
    private val adminAuditRecorder: AdminAuditRecorder,
    private val schedulerProperties: SchedulerProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun list(
        platform: LaunchAlertPlatform,
        email: String?,
        pageable: Pageable,
    ): Page<LaunchAlertSubscriptionResponse> {
        val keyword = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val subscriptions = if (keyword == null) {
            subscriptionRepository.findAllByPlatform(platform, pageable)
        } else {
            subscriptionRepository.findAllByPlatformAndEmailContaining(platform, keyword, pageable)
        }
        return subscriptions.map(LaunchAlertSubscriptionResponse::from)
    }

    /** 신청자가 문의로 철회를 요청했을 때 한 건만 지운다. */
    fun delete(adminId: Long, id: Long) {
        val subscription = subscriptionRepository.findById(id).orElseThrow { LaunchAlertSubscriptionNotFoundException() }
        subscriptionRepository.delete(subscription)
        record(adminId, AdminAuditAction.LAUNCH_ALERT_DELETE, id, subscription.platform.name)
    }

    @Transactional(readOnly = true)
    fun dispatches(pageable: Pageable): Page<LaunchAlertDispatchResponse> =
        dispatchRepository.findAll(pageable).map(LaunchAlertDispatchResponse::from)

    /**
     * 남은 신청 전체에 출시 안내를 보내도록 요청한다. 스케줄러 잡이 30초 안에 시작한다.
     * 발송 잡이 꺼져 있으면 요청이 영영 처리되지 않은 채 새 발송까지 막으므로 받지 않는다.
     */
    fun requestDispatch(adminId: Long, command: RequestLaunchAlertDispatchCommand): LaunchAlertDispatchResponse {
        if (!emailSender.available || !dispatchJobEnabled()) throw LaunchAlertMailUnavailableException()
        if (dispatchRepository.existsByPlatformAndStatus(command.platform, LaunchAlertDispatchStatus.DISPATCHING)) {
            throw LaunchAlertDispatchInProgressException()
        }
        val recipientCount = subscriptionRepository.countByPlatform(command.platform)
        if (recipientCount == 0L) throw LaunchAlertNoRecipientsException()

        val dispatch = try {
            dispatchRepository.saveAndFlush(
                LaunchAlertDispatch(platform = command.platform, storeUrl = command.storeUrl, requestedBy = adminId),
            )
        } catch (e: DataIntegrityViolationException) {
            // 동시에 들어온 요청이 먼저 만들었다 (플랫폼당 DISPATCHING 하나 제약)
            throw LaunchAlertDispatchInProgressException()
        }
        val dispatchId = requireNotNull(dispatch.id)
        record(adminId, AdminAuditAction.LAUNCH_ALERT_DISPATCH, dispatchId, "${command.platform.name} ${recipientCount}건")
        return LaunchAlertDispatchResponse.from(dispatch)
    }

    /** 보내는 중인 발송을 멈춘다. 이미 보낸 메일은 되돌릴 수 없고, 남은 신청은 그대로 둔다. */
    fun cancelDispatch(adminId: Long, id: Long): LaunchAlertDispatchResponse {
        val dispatch = dispatchRepository.findById(id).orElseThrow { LaunchAlertDispatchNotFoundException() }
        if (!dispatch.dispatching) throw LaunchAlertDispatchNotCancelableException()
        dispatch.cancel(Instant.now(clock))
        dispatchRepository.flush()
        record(adminId, AdminAuditAction.LAUNCH_ALERT_DISPATCH_CANCEL, id, dispatch.platform.name)
        return LaunchAlertDispatchResponse.from(dispatch)
    }

    /**
     * 전체 발송 전에 관리자 본인 주소로 같은 메일을 한 통 보내 본다. 신청 목록은 건드리지 않는다.
     * SMTP 응답을 기다리는 동안 DB 커넥션을 쥐지 않도록 트랜잭션 밖에서 보낸다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun sendTestMail(adminId: Long, command: SendLaunchAlertTestMailCommand) {
        if (!emailSender.available) throw LaunchAlertMailUnavailableException()
        // 실패한 시도도 남도록 보내기 전에 기록한다. 받는 주소는 남기지 않는다.
        record(adminId, AdminAuditAction.LAUNCH_ALERT_TEST_SEND, 0, command.platform.name)
        try {
            emailSender.send(mailComposer.compose(command.platform, command.storeUrl, command.email.trim()))
        } catch (e: EmailSendException) {
            log.warn("출시 안내 테스트 메일 발송 실패. permanent={}, reason={}", e.permanent, e.reason)
            throw LaunchAlertMailSendFailedException()
        }
    }

    private fun dispatchJobEnabled(): Boolean =
        schedulerProperties.enabled && schedulerProperties.notification.launchAlertDispatch.enabled

    private fun record(adminId: Long, action: AdminAuditAction, targetId: Long, memo: String) {
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = action,
                targetType = AdminAuditTargetType.LAUNCH_ALERT,
                targetId = targetId,
                memo = memo,
            ),
        )
    }
}
