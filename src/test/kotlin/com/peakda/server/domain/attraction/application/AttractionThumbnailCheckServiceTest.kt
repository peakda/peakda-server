package com.peakda.server.domain.attraction.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.attraction.repository.AttractionThumbnailCheckTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AttractionThumbnailCheckServiceTest {
    private val repository = mock(AttractionRepository::class.java)
    private val now = Instant.parse("2026-10-06T00:00:00Z")
    private val service = AttractionThumbnailCheckService(repository, AttractionEligibilityProperties(), Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `미확인 명소를 먼저 고르고 남는 몫만큼 확인한 지 오래된 명소를 더한다`() {
        `when`(repository.findThumbnailUncheckedTargets(setOf("12"), PageRequest.of(0, 3))).thenReturn(listOf(target(1)))
        `when`(repository.findThumbnailCheckedBefore(setOf("12"), now.minus(Duration.ofDays(30)), PageRequest.of(0, 2)))
            .thenReturn(listOf(target(2), target(3)))

        val targets = service.findTargets(3, Duration.ofDays(30))

        assertThat(targets.map { it.attractionId }).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `미확인 명소로 한도가 차면 다시 확인할 명소는 조회하지 않는다`() {
        `when`(repository.findThumbnailUncheckedTargets(setOf("12"), PageRequest.of(0, 2))).thenReturn(listOf(target(1), target(2)))

        val targets = service.findTargets(2, Duration.ofDays(30))

        assertThat(targets).hasSize(2)
        verify(repository).findThumbnailUncheckedTargets(setOf("12"), PageRequest.of(0, 2))
        verifyNoMoreInteractions(repository)
    }

    private fun target(id: Long) = AttractionThumbnailCheckTarget(id, "http://tong.visitkorea.or.kr/cms/resource/30/${id}_image3_1.jpg")
}
