package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordInvalidStatusException
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SpotRecordModerationService(
    private val spotRecordRepository: SpotRecordRepository,
) {

    fun hide(recordId: Long) {
        val record = find(recordId)
        when (record.status) {
            SpotRecordStatus.PUBLISHED -> record.status = SpotRecordStatus.HIDDEN
            SpotRecordStatus.HIDDEN -> Unit
            SpotRecordStatus.DRAFT -> throw SpotRecordInvalidStatusException()
        }
    }

    fun restore(recordId: Long) {
        val record = find(recordId)
        when (record.status) {
            SpotRecordStatus.HIDDEN -> record.status = SpotRecordStatus.PUBLISHED
            SpotRecordStatus.PUBLISHED -> Unit
            SpotRecordStatus.DRAFT -> throw SpotRecordInvalidStatusException()
        }
    }

    @Transactional(readOnly = true)
    fun summaries(recordIds: List<Long>): List<SpotRecordModerationSummary> {
        if (recordIds.isEmpty()) return emptyList()
        return spotRecordRepository.findAllById(recordIds.distinct()).map { record ->
            SpotRecordModerationSummary(
                id = requireNotNull(record.id),
                userId = record.userId,
                status = record.status,
                memo = record.memo?.take(MEMO_SUMMARY_LENGTH),
                visitedDate = record.visitedDate,
            )
        }
    }

    private fun find(recordId: Long): SpotRecord =
        spotRecordRepository.findById(recordId).orElseThrow { SpotRecordNotFoundException() }

    companion object {
        private const val MEMO_SUMMARY_LENGTH = 80
    }
}
