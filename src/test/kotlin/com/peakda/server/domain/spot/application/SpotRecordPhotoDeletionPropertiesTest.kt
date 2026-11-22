package com.peakda.server.domain.spot.application

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class SpotRecordPhotoDeletionPropertiesTest {
    @Test fun `기본값은 유한 횟수와 capped exponential backoff 및 lease를 보장한다`() {
        val p = SpotRecordPhotoDeletionProperties()
        assertThat(p.maxAttempts).isEqualTo(5)
        assertThat((1..5).map { p.backoff(it).seconds }).containsExactly(30L, 60L, 120L, 240L, 480L)
        assertThat(p.backoff(100)).isEqualTo(Duration.ofHours(1))
        assertThat(p.lease).isEqualTo(Duration.ofMinutes(2))
        assertThat(p.batchSize).isEqualTo(50)
    }
    @Test fun `무한 또는 음수 설정을 기동 시 거부한다`() {
        assertThatThrownBy { SpotRecordPhotoDeletionProperties(maxAttempts = 0) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { SpotRecordPhotoDeletionProperties(lease = Duration.ZERO) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { SpotRecordPhotoDeletionProperties(initialBackoff = Duration.ofSeconds(-1)) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { SpotRecordPhotoDeletionProperties(batchSize = 1001) }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
