package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.exception.LaunchAlertSubscriptionNotFoundException
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertPurgeResponse
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertRecipientsResponse
import com.peakda.server.domain.launchalert.presentation.response.LaunchAlertSubscriptionResponse
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * 출시 알림 신청 관리. 메일은 관리자가 내려받은 목록으로 직접 보내고, 보낸 뒤 [purge] 로 파기한다.
 * 파기는 내려받은 신청만 지우므로, 내려받은 뒤 들어온 신청은 다음 발송 때까지 남는다.
 *
 * 감사 로그 memo 에는 이메일을 남기지 않는다. 파기한 개인정보가 감사 로그에 남으면 파기가 아니다.
 */
@Service
@Transactional
class LaunchAlertAdminService(
    private val repository: LaunchAlertSubscriptionRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
    private val clock: Clock = Clock.systemUTC(),
) {

    @Transactional(readOnly = true)
    fun list(
        platform: LaunchAlertPlatform,
        email: String?,
        pageable: Pageable,
    ): Page<LaunchAlertSubscriptionResponse> {
        val keyword = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val subscriptions = if (keyword == null) {
            repository.findAllByPlatform(platform, pageable)
        } else {
            repository.findAllByPlatformAndEmailContaining(platform, keyword, pageable)
        }
        return subscriptions.map(LaunchAlertSubscriptionResponse::from)
    }

    /**
     * 남아 있는 신청 전체를 신청 순으로 돌려주고, 돌려준 신청에 내려받은 시각을 찍는다.
     * 파일을 잃어버려 다시 내려받아도 이미 내려받은 신청은 그대로 다시 포함된다.
     */
    fun recipients(adminId: Long, platform: LaunchAlertPlatform): LaunchAlertRecipientsResponse {
        val subscriptions = repository.findAllByPlatformOrderByIdAsc(platform)
        if (subscriptions.isNotEmpty()) {
            repository.markExported(subscriptions.map { requireNotNull(it.id) }.toLongArray(), Instant.now(clock))
        }
        record(adminId, AdminAuditAction.LAUNCH_ALERT_EXPORT, "${platform.name} ${subscriptions.size}건")
        return LaunchAlertRecipientsResponse(
            platform = platform,
            count = subscriptions.size,
            emails = subscriptions.map { it.email },
        )
    }

    /** 내려받은 신청을 지운다. 내려받은 뒤 들어온 신청은 남는다. */
    fun purge(adminId: Long, platform: LaunchAlertPlatform): LaunchAlertPurgeResponse {
        val deletedCount = repository.deleteExported(platform)
        record(adminId, AdminAuditAction.LAUNCH_ALERT_PURGE, "${platform.name} ${deletedCount}건")
        return LaunchAlertPurgeResponse(deletedCount)
    }

    /** 신청자가 문의로 철회를 요청했을 때 한 건만 지운다. */
    fun delete(adminId: Long, id: Long) {
        val subscription = repository.findById(id).orElseThrow { LaunchAlertSubscriptionNotFoundException() }
        repository.delete(subscription)
        record(adminId, AdminAuditAction.LAUNCH_ALERT_DELETE, subscription.platform.name, targetId = id)
    }

    /** 목록 단위 조치(내려받기·파기)는 대상 행이 여럿이라 targetId 를 0 으로 둔다. */
    private fun record(adminId: Long, action: AdminAuditAction, memo: String, targetId: Long = 0) {
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
