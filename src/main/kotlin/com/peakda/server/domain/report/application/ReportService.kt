package com.peakda.server.domain.report.application

import com.peakda.server.domain.report.entity.ReportReason
import com.peakda.server.domain.report.entity.ReportTargetType
import com.peakda.server.domain.report.exception.SelfReportNotAllowedException
import com.peakda.server.domain.report.repository.ReportRepository
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * UGC 신고(SCR-023/024h, P2-4). V1 은 스팟 기록(게시글)만 신고 대상이며, 관리자 심사 API 는 범위 밖이다.
 */
@Service
@Transactional
class ReportService(
    private val reportRepository: ReportRepository,
    private val spotRecordRepository: SpotRecordRepository,
) {

    fun create(reporterId: Long, targetType: ReportTargetType, targetId: Long, reason: ReportReason, detail: String?) {
        when (targetType) {
            ReportTargetType.SPOT_RECORD -> validateSpotRecordTarget(reporterId, targetId)
        }
        // ON CONFLICT DO NOTHING — 같은 대상을 이미 신고했으면 무시되어 동시 요청에서도 단일 행이 보장된다.
        reportRepository.insertIfAbsent(reporterId, targetType.name, targetId, reason.name, detail)
    }

    /** 게시된 기록만 신고 가능 — DRAFT 존재 자체를 숨기려 404 로 통일한다. */
    private fun validateSpotRecordTarget(reporterId: Long, targetId: Long) {
        val record = spotRecordRepository.findById(targetId).orElseThrow { SpotRecordNotFoundException() }
        if (record.status != SpotRecordStatus.PUBLISHED) throw SpotRecordNotFoundException()
        if (record.userId == reporterId) throw SelfReportNotAllowedException()
    }
}
