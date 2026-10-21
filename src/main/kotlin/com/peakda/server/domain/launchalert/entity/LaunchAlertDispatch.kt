package com.peakda.server.domain.launchalert.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 출시 안내 메일 발송 1회. 관리자가 만들고 스케줄러 잡이 [cursorId] 다음 신청부터 묶음 단위로 보낸다.
 *
 * 커서를 발송 건에 두는 이유: 잡이 중간에 끊겨도 이어서 보내고, 실패한 신청을 같은 발송에서 반복해 보내지 않는다.
 * 보내는 중에 들어온 신청도 id 가 커서보다 크면 이 발송에 포함된다. 다만 id 를 받은 뒤 늦게 커밋돼 커서보다
 * 작은 id 로 들어온 신청은 이 발송에서 빠지고 목록에 남아 다음 발송 대상이 된다.
 */
@Entity
@Table(name = "launch_alert_dispatches")
class LaunchAlertDispatch(
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, columnDefinition = "TEXT")
    val platform: LaunchAlertPlatform,

    @Column(name = "store_url", nullable = false, columnDefinition = "TEXT")
    val storeUrl: String,

    @Column(name = "requested_by", nullable = false)
    val requestedBy: Long,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "TEXT")
    var status: LaunchAlertDispatchStatus = LaunchAlertDispatchStatus.DISPATCHING
        protected set

    @Column(name = "cursor_id", nullable = false)
    var cursorId: Long = 0
        protected set

    @Column(name = "sent_count", nullable = false)
    var sentCount: Int = 0
        protected set

    @Column(name = "failed_count", nullable = false)
    var failedCount: Int = 0
        protected set

    @Column(name = "completed_at")
    var completedAt: Instant? = null
        protected set

    val dispatching: Boolean
        get() = status == LaunchAlertDispatchStatus.DISPATCHING

    /** 이 신청에 보내기 직전에 커서를 옮긴다. 보낸 뒤 끊기더라도 이 발송에서 같은 사람에게 다시 보내지 않는다. */
    fun moveCursor(subscriptionId: Long) {
        check(dispatching) { "발송 중이 아닌 발송 건은 진행할 수 없다" }
        cursorId = subscriptionId
    }

    /** 결과 기록은 상태를 보지 않는다. 보내는 사이 취소돼도 이미 보낸 메일은 세어야 한다. */
    fun recordSent() {
        sentCount++
    }

    fun recordFailed() {
        failedCount++
    }

    fun complete(now: Instant) = finish(LaunchAlertDispatchStatus.COMPLETED, now)

    fun fail(now: Instant) = finish(LaunchAlertDispatchStatus.FAILED, now)

    fun cancel(now: Instant) = finish(LaunchAlertDispatchStatus.CANCELED, now)

    private fun finish(status: LaunchAlertDispatchStatus, now: Instant) {
        check(dispatching) { "이미 끝난 발송 건이다" }
        this.status = status
        completedAt = now
    }
}
