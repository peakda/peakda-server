package com.peakda.server.domain.launchalert.application

import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import com.peakda.server.domain.launchalert.exception.LaunchAlertConsentRequiredException
import com.peakda.server.domain.launchalert.exception.LaunchAlertRateLimitException
import com.peakda.server.domain.launchalert.repository.LaunchAlertSubscriptionRepository
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class LaunchAlertServiceTest {

    private val repository = mock(LaunchAlertSubscriptionRepository::class.java)
    private val rateLimiter = mock(LaunchAlertRateLimiter::class.java)
    private val service = LaunchAlertService(
        repository = repository,
        rateLimiter = rateLimiter,
        properties = LaunchAlertProperties(consentVersion = "v1"),
        clock = Clock.fixed(NOW, ZoneOffset.UTC),
    )

    @Test
    fun `이메일을 정규화하고 현재 동의 문구 버전으로 저장한다`() {
        `when`(rateLimiter.tryAcquire(IP)).thenReturn(true)

        service.subscribe(command(email = "  Ex1@XXX.com "))

        verify(repository).upsert("IOS", "ex1@xxx.com", "v1", NOW)
    }

    @Test
    fun `동의하지 않으면 횟수를 쓰지 않고 거절한다`() {
        assertThatThrownBy { service.subscribe(command(privacyConsent = false)) }
            .isInstanceOf(LaunchAlertConsentRequiredException::class.java)

        verifyNoInteractions(rateLimiter, repository)
    }

    @Test
    fun `횟수를 넘긴 IP 는 저장하지 않는다`() {
        `when`(rateLimiter.tryAcquire(IP)).thenReturn(false)

        assertThatThrownBy { service.subscribe(command()) }
            .isInstanceOf(LaunchAlertRateLimitException::class.java)

        verifyNoInteractions(repository)
    }

    private fun command(email: String = "ex1@xxx.com", privacyConsent: Boolean = true) =
        SubscribeLaunchAlertCommand(
            platform = LaunchAlertPlatform.IOS,
            email = email,
            privacyConsent = privacyConsent,
            clientIp = IP,
        )

    companion object {
        private const val IP = "203.0.113.7"
        private val NOW = Instant.parse("2026-10-21T03:00:00Z")
    }
}
