package com.peakda.server.domain.auth.apple.application

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 탈퇴가 커밋된 뒤에만 애플 토큰을 폐기한다. 탈퇴 트랜잭션이 애플 응답을 기다리며 DB 커넥션을
 * 붙잡지 않도록 별도 스레드에서 부른다 (AuthSessionRevokedListener 와 같은 방식).
 */
@Component
class AppleTokenRevokeListener(
    private val appleTokenService: AppleTokenService,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAppleUserWithdrawn(event: AppleUserWithdrawnEvent) {
        appleTokenService.revoke(event.userId, event.appleUserId)
    }
}
