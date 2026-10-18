package com.peakda.server.domain.notification.application

import com.peakda.server.domain.auth.session.application.AuthSessionRevokedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 로그인 세션이 끝난 기기의 푸시 토큰을 지운다. 세션 해제가 커밋된 뒤에만 지우고,
 * 커밋이 끝난 트랜잭션에 묶이지 않도록 별도 스레드에서 [DeviceTokenService] 의 트랜잭션으로 실행한다.
 */
@Component
class AuthSessionRevokedListener(
    private val deviceTokenService: DeviceTokenService,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onSessionRevoked(event: AuthSessionRevokedEvent) {
        deviceTokenService.deleteByDevice(event.userId, event.deviceId)
    }
}
