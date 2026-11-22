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

class SpotRecordModerationServiceTest {

    private val repository = mock(SpotRecordRepository::class.java)
    private val service = SpotRecordModerationService(repository)

    @Test
    fun `게시된 기록을 숨기고 숨긴 기록을 복구한다`() {
        val record = record(SpotRecordStatus.PUBLISHED)
        `when`(repository.findByIdForUpdate(RECORD_ID)).thenReturn(record)

        service.hide(RECORD_ID)
        assertThat(record.status).isEqualTo(SpotRecordStatus.HIDDEN)

        service.restore(RECORD_ID)
        assertThat(record.status).isEqualTo(SpotRecordStatus.PUBLISHED)
    }

    @Test
    fun `초안 기록은 관리자 노출 상태를 바꿀 수 없다`() {
        val record = record(SpotRecordStatus.DRAFT)
        `when`(repository.findByIdForUpdate(RECORD_ID)).thenReturn(record)

        assertThatThrownBy { service.hide(RECORD_ID) }
            .isInstanceOf(SpotRecordInvalidStatusException::class.java)
    }

    @Test
    fun `숨김과 복구 재요청은 멱등이고 최초 게시 시각을 보존한다`() {
        val publishedAt = java.time.Instant.parse("2026-11-01T00:00:00Z")
        val record = record(SpotRecordStatus.PUBLISHED).apply { this.publishedAt = publishedAt }
        `when`(repository.findByIdForUpdate(RECORD_ID)).thenReturn(record)
        repeat(2) { service.hide(RECORD_ID) }
        assertThat(record.status).isEqualTo(SpotRecordStatus.HIDDEN)
        repeat(2) { service.restore(RECORD_ID) }
        assertThat(record.status).isEqualTo(SpotRecordStatus.PUBLISHED)
        assertThat(record.publishedAt).isEqualTo(publishedAt)
    }

    @Test
    fun `초안 복구와 없는 기록은 기존 오류를 반환한다`() {
        `when`(repository.findByIdForUpdate(RECORD_ID)).thenReturn(record(SpotRecordStatus.DRAFT))
        assertThatThrownBy { service.restore(RECORD_ID) }.isInstanceOf(SpotRecordInvalidStatusException::class.java)
        `when`(repository.findByIdForUpdate(RECORD_ID)).thenReturn(null)
        assertThatThrownBy { service.hide(RECORD_ID) }
            .isInstanceOf(com.peakda.server.domain.spot.exception.SpotRecordNotFoundException::class.java)
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
