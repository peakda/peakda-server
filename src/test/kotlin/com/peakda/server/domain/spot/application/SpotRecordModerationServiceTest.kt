package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordInvalidStatusException
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.util.Optional

class SpotRecordModerationServiceTest {

    private val repository = mock(SpotRecordRepository::class.java)
    private val service = SpotRecordModerationService(repository)

    @Test
    fun `게시된 기록을 숨기고 숨긴 기록을 복구한다`() {
        val record = record(SpotRecordStatus.PUBLISHED)
        `when`(repository.findById(RECORD_ID)).thenReturn(Optional.of(record))

        service.hide(RECORD_ID)
        assertThat(record.status).isEqualTo(SpotRecordStatus.HIDDEN)

        service.restore(RECORD_ID)
        assertThat(record.status).isEqualTo(SpotRecordStatus.PUBLISHED)
    }

    @Test
    fun `초안 기록은 관리자 노출 상태를 바꿀 수 없다`() {
        val record = record(SpotRecordStatus.DRAFT)
        `when`(repository.findById(RECORD_ID)).thenReturn(Optional.of(record))

        assertThatThrownBy { service.hide(RECORD_ID) }
            .isInstanceOf(SpotRecordInvalidStatusException::class.java)
    }

    @Test
    fun `요약은 메모를 80자로 자르고 요청 id를 한 번에 조회한다`() {
        val record = record(SpotRecordStatus.PUBLISHED, "가".repeat(100))
        `when`(repository.findAllById(listOf(RECORD_ID))).thenReturn(listOf(record))

        val summary = service.summaries(listOf(RECORD_ID, RECORD_ID)).single()

        assertThat(summary.memo).hasSize(80)
        assertThat(summary.visitedDate).isEqualTo(LocalDate.of(2026, 7, 28))
    }

    private fun record(status: SpotRecordStatus, memo: String? = "기록 메모"): SpotRecord {
        val record = SpotRecord(
            spotId = 10L,
            userId = 20L,
            visitedDate = LocalDate.of(2026, 7, 28),
            memo = memo,
            status = status,
        )
        ReflectionTestUtils.setField(record, "id", RECORD_ID)
        return record
    }

    companion object {
        private const val RECORD_ID = 1024L
    }
}
