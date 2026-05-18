package com.peakda.server.infrastructure.external.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class QuotaServiceTest {

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-16T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    @Test
    fun `disabled 인 경우 무조건 Allowed 를 반환한다`() {
        val template = stubTemplate()
        val service = QuotaService(
            redis = template,
            properties = ExternalQuotaProperties(enabled = false),
            clock = clock,
        )

        val decision = service.tryConsume("KTO", "KorService2")

        assertThat(decision).isInstanceOf(QuotaService.Decision.Allowed::class.java)
        Mockito.verify(template.opsForValue(), Mockito.never()).increment(anyString())
    }

    @Test
    fun `한도 미만이면 INCR 하고 Allowed 를 반환한다`() {
        val template = stubTemplate(initialCount = 5L)
        val service = QuotaService(
            redis = template,
            properties = ExternalQuotaProperties(
                enabled = true,
                services = mapOf("KTO.KorService2" to ExternalQuotaProperties.ServiceQuota(dailyLimit = 10)),
            ),
            clock = clock,
        )

        val decision = service.tryConsume("KTO", "KorService2")

        assertThat(decision).isInstanceOf(QuotaService.Decision.Allowed::class.java)
        assertThat((decision as QuotaService.Decision.Allowed).used).isEqualTo(5L)
    }

    @Test
    fun `한도를 초과하면 DECR 로 되돌리고 Exhausted 를 반환한다`() {
        val template = stubTemplate(initialCount = 11L)
        val service = QuotaService(
            redis = template,
            properties = ExternalQuotaProperties(
                enabled = true,
                services = mapOf("KTO.KorService2" to ExternalQuotaProperties.ServiceQuota(dailyLimit = 10)),
            ),
            clock = clock,
        )

        val decision = service.tryConsume("KTO", "KorService2")

        assertThat(decision).isInstanceOf(QuotaService.Decision.Exhausted::class.java)
        assertThat((decision as QuotaService.Decision.Exhausted).used).isEqualTo(10L)
        Mockito.verify(template.opsForValue()).decrement(anyString())
    }

    @Test
    fun `첫 INCR 일 때 25h TTL 을 설정한다`() {
        val template = stubTemplate(initialCount = 1L)
        val service = QuotaService(
            redis = template,
            properties = ExternalQuotaProperties(
                enabled = true,
                services = mapOf("KMA.VilageFcstInfoService" to ExternalQuotaProperties.ServiceQuota(dailyLimit = 100)),
            ),
            clock = clock,
        )

        service.tryConsume("KMA", "VilageFcstInfoService")

        Mockito.verify(template).expire(anyString(), eq(Duration.ofHours(25)))
    }

    private fun stubTemplate(initialCount: Long = 1L): StringRedisTemplate {
        val template = Mockito.mock(StringRedisTemplate::class.java)
        @Suppress("UNCHECKED_CAST")
        val ops = Mockito.mock(ValueOperations::class.java) as ValueOperations<String, String>
        Mockito.`when`(template.opsForValue()).thenReturn(ops)
        Mockito.`when`(ops.increment(anyString())).thenReturn(initialCount)
        Mockito.`when`(ops.decrement(anyString())).thenReturn(initialCount - 1L)
        Mockito.`when`(template.expire(anyString(), any(Duration::class.java))).thenReturn(true)
        return template
    }
}
