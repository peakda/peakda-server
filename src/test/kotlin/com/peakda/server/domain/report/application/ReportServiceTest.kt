package com.peakda.server.domain.report.application

import com.peakda.server.domain.report.entity.ReportReason
import com.peakda.server.domain.report.entity.ReportTargetType
import com.peakda.server.domain.report.exception.SelfReportNotAllowedException
import com.peakda.server.domain.report.repository.ReportRepository
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class ReportServiceTest {

    private val reportRepository = mock(ReportRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)

    private val service = ReportService(reportRepository, spotRecordRepository)

    @Test
    fun `대상 기록이 없으면 SpotRecordNotFoundException 이다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty())

        assertThatThrownBy {
            service.create(REPORTER_ID, ReportTargetType.SPOT_RECORD, RECORD_ID, ReportReason.SPAM, null)
        }.isInstanceOf(SpotRecordNotFoundException::class.java)
    }

    @Test
    fun `DRAFT 기록은 신고할 수 없다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.DRAFT, OWNER_ID)))

        assertThatThrownBy {
            service.create(REPORTER_ID, ReportTargetType.SPOT_RECORD, RECORD_ID, ReportReason.SPAM, null)
        }.isInstanceOf(SpotRecordNotFoundException::class.java)
    }

    @Test
    fun `본인 게시글은 신고할 수 없다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED, REPORTER_ID)))

        assertThatThrownBy {
            service.create(REPORTER_ID, ReportTargetType.SPOT_RECORD, RECORD_ID, ReportReason.SPAM, null)
        }.isInstanceOf(SelfReportNotAllowedException::class.java)
    }

    @Test
    fun `정상 신고는 저장소에 위임한다`() {
        `when`(spotRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(record(SpotRecordStatus.PUBLISHED, OWNER_ID)))

        service.create(REPORTER_ID, ReportTargetType.SPOT_RECORD, RECORD_ID, ReportReason.HARASSMENT, "괴롭힘 신고입니다")

        verify(reportRepository).insertIfAbsent(REPORTER_ID, "SPOT_RECORD", RECORD_ID, "HARASSMENT", "괴롭힘 신고입니다")
    }

    private fun record(status: SpotRecordStatus, ownerId: Long): SpotRecord {
        val record = SpotRecord(spotId = 100L, userId = ownerId, status = status)
        ReflectionTestUtils.setField(record, "id", RECORD_ID)
        return record
    }

    companion object {
        private const val REPORTER_ID = 1L
        private const val OWNER_ID = 2L
        private const val RECORD_ID = 500L
    }
}
