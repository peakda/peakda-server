package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatch
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatchStatus
import com.peakda.server.domain.launchalert.exception.LaunchAlertMailTemplateNotFoundException
import com.peakda.server.domain.launchalert.repository.LaunchAlertDispatchRepository
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import com.peakda.server.infrastructure.mail.EmailSendException
import com.peakda.server.infrastructure.mail.EmailSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant

/**
 * 발송 중인 출시 안내를 한 묶음(최대 100건)씩 보낸다. 스케줄러 잡이 30초마다 부른다.
 *
 * 한 번에 끝까지 보내지 않는 이유: 스케줄러 스레드가 하나라 오래 붙잡으면 다른 잡이 모두 밀린다.
 *
 * 수신자마다 "커서 이동 → 발송 → 신청 삭제·건수 기록" 순서로 진행을 바로 저장한다. 보내기 전에 커서를 옮기므로
 * 배포·DB 장애로 중간에 끊겨도 같은 발송에서 같은 사람에게 두 번 보내지 않는다. 끊긴 그 한 명은 신청이 남아
 * 다음 발송 대상이 된다. SMTP 응답을 기다리는 동안에는 트랜잭션을 열어 두지 않는다.
 *
 * 주소 형식 오류·수신 거부처럼 다시 보내도 받을 수 없는 신청은 목적을 이룰 수 없으므로 실패로 세고 바로 지운다
 * (안내 문구 "발송 후 지체 없이 파기"). 연결 오류 같은 일시 실패만 다시 보낼 수 있게 남긴다.
 */
@Service
class LaunchAlertFanoutService(
    private val dispatchRepository: LaunchAlertDispatchRepository,
    private val subscriptionRepository: LaunchAlertSubscriptionRepository,
    private val mailComposer: LaunchAlertMailComposer,
    private val mailTemplateService: LaunchAlertMailTemplateService,
    private val emailSender: EmailSender,
    private val properties: LaunchAlertProperties,
    transactionManager: PlatformTransactionManager,
    private val clock: Clock = Clock.systemUTC(),
    private val pause: (Long) -> Unit = Thread::sleep,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun hasPendingDispatch(): Boolean = dispatchRepository.existsByStatus(LaunchAlertDispatchStatus.DISPATCHING)

    /** @return 이번 묶음에서 보낸 메일 수 */
    fun dispatchNextBatch(): Int {
        val dispatch = dispatchRepository.findFirstByStatusOrderByIdAsc(LaunchAlertDispatchStatus.DISPATCHING) ?: return 0
        val dispatchId = requireNotNull(dispatch.id)

        if (!emailSender.available) {
            log.warn("메일 발송이 꺼져 있어 출시 안내 발송을 중단한다. dispatchId={}", dispatchId)
            update(dispatchId) { if (it.dispatching) it.fail(Instant.now(clock)) }
            return 0
        }

        val subscriptions = subscriptionRepository.findTop100ByPlatformAndIdGreaterThanOrderByIdAsc(
            platform = dispatch.platform,
            id = dispatch.cursorId,
        )
        if (subscriptions.isEmpty()) {
            update(dispatchId) { if (it.dispatching) it.complete(Instant.now(clock)) }
            return 0
        }
        // 발송 중에는 문구 수정을 막으므로 묶음마다 한 번 읽으면 된다.
        val content = try {
            mailTemplateService.content(dispatch.platform)
        } catch (e: LaunchAlertMailTemplateNotFoundException) {
            log.warn("출시 안내 문구가 없어 발송을 중단한다. dispatchId={}", dispatchId)
            update(dispatchId) { if (it.dispatching) it.fail(Instant.now(clock)) }
            return 0
        }
        var sent = 0
        var transientFailures = 0

        for (subscription in subscriptions) {
            val subscriptionId = requireNotNull(subscription.id)
            // 관리자가 취소했으면 여기서 멈춘다.
            val stillDispatching = update(dispatchId) { managed ->
                managed.dispatching.also { if (it) managed.moveCursor(subscriptionId) }
            }
            if (!stillDispatching) return sent

            try {
                emailSender.send(mailComposer.compose(content, dispatch.storeUrl, subscription.email))
            } catch (e: EmailSendException) {
                // 예외 객체를 넘기지 않는다. 원인 메시지에 수신 주소가 들어 있다.
                log.warn(
                    "출시 안내 메일 발송 실패. dispatchId={}, subscriptionId={}, permanent={}, reason={}",
                    dispatchId, subscriptionId, e.permanent, e.reason,
                )
                transactionTemplate.executeWithoutResult {
                    if (e.permanent) subscriptionRepository.deleteSent(subscriptionId)
                    dispatchRepository.findById(dispatchId).orElseThrow().recordFailed()
                }
                // 주소 하나의 문제는 SMTP 장애 판단에 넣지 않는다. 앞쪽의 잘못된 주소 몇 개가 발송 전체를 막지 않게 한다.
                if (!e.permanent && ++transientFailures >= properties.dispatch.maxConsecutiveFailures) {
                    update(dispatchId) { if (it.dispatching) it.fail(Instant.now(clock)) }
                    return sent
                }
                continue
            }

            transactionTemplate.executeWithoutResult {
                subscriptionRepository.deleteSent(subscriptionId)
                dispatchRepository.findById(dispatchId).orElseThrow().recordSent()
            }
            sent++
            transientFailures = 0
            if (!pauseBetweenMails()) return sent
        }

        if (subscriptions.size < BATCH_SIZE) {
            update(dispatchId) { if (it.dispatching) it.complete(Instant.now(clock)) }
        }
        return sent
    }

    /** 종료 중 인터럽트되면 진행은 이미 저장돼 있으니 여기서 멈추고 다음 실행에 맡긴다. */
    private fun pauseBetweenMails(): Boolean =
        try {
            pause(properties.dispatch.sendInterval.toMillis())
            true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }

    private fun <T> update(dispatchId: Long, action: (LaunchAlertDispatch) -> T): T =
        requireNotNull(transactionTemplate.execute { action(dispatchRepository.findById(dispatchId).orElseThrow()) })

    companion object {
        /** [LaunchAlertSubscriptionRepository.findTop100ByPlatformAndIdGreaterThanOrderByIdAsc] 의 묶음 크기 */
        const val BATCH_SIZE = 100
    }
}
