package com.peakda.server.domain.spot.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("spot.photo-deletion")
data class SpotRecordPhotoDeletionProperties(
    val enabled: Boolean = true,
    val batchSize: Int = 50,
    val maxAttempts: Int = 5,
    val initialBackoff: Duration = Duration.ofSeconds(30),
    val maxBackoff: Duration = Duration.ofHours(1),
    val lease: Duration = Duration.ofMinutes(2),
) {
    init {
        require(batchSize in 1..1000 && maxAttempts in 1..100)
        require(!initialBackoff.isNegative && !initialBackoff.isZero && maxBackoff >= initialBackoff)
        require(!lease.isNegative && !lease.isZero)
    }

    fun backoff(attempts: Int): Duration {
        var delay = initialBackoff
        repeat((attempts - 1).coerceIn(0, 100)) {
            delay = if (delay >= maxBackoff.dividedBy(2)) maxBackoff else delay.multipliedBy(2)
        }
        return delay.coerceAtMost(maxBackoff)
    }
}
