package com.peakda.server.domain.location.application

import com.peakda.server.domain.location.entity.LocationAccessChannel
import com.peakda.server.domain.location.entity.LocationServiceType
import com.peakda.server.domain.location.entity.LocationUsageLog
import com.peakda.server.domain.location.repository.LocationUsageLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

class LocationUsageRecorderTest {

    private val locationUsageLogRepository = mock(LocationUsageLogRepository::class.java)
    private val locationUsageDebouncer = mock(LocationUsageDebouncer::class.java)
    private val recorder = LocationUsageRecorder(locationUsageLogRepository, locationUsageDebouncer)

    private val command = RecordLocationUsageCommand(
        userId = 7L,
        channel = LocationAccessChannel.ANDROID,
        service = LocationServiceType.BLOOM_MAP,
        usedAt = Instant.parse("2026-08-17T12:49:24Z"),
    )

    @Test
    fun `디바운스를 통과하면 확인자료를 저장한다`() {
        `when`(locationUsageDebouncer.shouldRecord(7L, LocationServiceType.BLOOM_MAP)).thenReturn(true)

        recorder.record(command)

        val captor = ArgumentCaptor.forClass(LocationUsageLog::class.java)
        verify(locationUsageLogRepository).save(captor.capture())
        val saved = captor.value
        assertThat(saved.userId).isEqualTo(7L)
        assertThat(saved.channel).isEqualTo(LocationAccessChannel.ANDROID)
        assertThat(saved.service).isEqualTo(LocationServiceType.BLOOM_MAP)
        assertThat(saved.usedAt).isEqualTo(Instant.parse("2026-08-17T12:49:24Z"))
    }

    @Test
    fun `디바운스 구간 안의 재요청은 저장하지 않는다`() {
        `when`(locationUsageDebouncer.shouldRecord(7L, LocationServiceType.BLOOM_MAP)).thenReturn(false)

        recorder.record(command)

        verify(locationUsageLogRepository, never()).save(any())
    }

    @Test
    fun `저장이 실패해도 예외를 밖으로 던지지 않는다`() {
        `when`(locationUsageDebouncer.shouldRecord(7L, LocationServiceType.BLOOM_MAP)).thenReturn(true)
        `when`(locationUsageLogRepository.save(any<LocationUsageLog>()))
            .thenThrow(IllegalStateException("db down"))

        assertThatCode { recorder.record(command) }.doesNotThrowAnyException()
    }
}
