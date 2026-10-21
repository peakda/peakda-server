package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.exception.LaunchAlertConsentRequiredException
import com.peakda.server.domain.launchalert.exception.LaunchAlertRateLimitException
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class LaunchAlertService(
    private val repository: LaunchAlertSubscriptionRepository,
    private val rateLimiter: LaunchAlertRateLimiter,
    private val properties: LaunchAlertProperties,
    private val clock: Clock = Clock.systemUTC(),
) {

    /**
     * 출시 알림을 신청한다. 이미 신청한 이메일이면 동의 시각·버전만 갱신한다.
     *
     * 신규와 재신청을 같은 응답으로 돌려준다. 응답이 다르면 특정 이메일의 신청 여부를 알아낼 수 있다.
     * 트랜잭션을 걸지 않는다. 비로그인 요청이 Redis 응답을 기다리는 동안 DB 커넥션을 쥐고 있으면 안 된다.
     */
    fun subscribe(command: SubscribeLaunchAlertCommand) {
        if (!command.privacyConsent) throw LaunchAlertConsentRequiredException()
        if (!rateLimiter.tryAcquire(command.clientIp)) throw LaunchAlertRateLimitException()

        repository.upsert(
            platform = command.platform.name,
            email = command.email.trim().lowercase(),
            consentVersion = properties.consentVersion,
            consentedAt = Instant.now(clock),
        )
    }
}
