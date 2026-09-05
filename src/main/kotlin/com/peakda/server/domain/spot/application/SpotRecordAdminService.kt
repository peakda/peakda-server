package com.peakda.server.domain.spot.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.SpotRecordInvalidStatusException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SpotRecordAdminService(
    private val spotRecordModerationService: SpotRecordModerationService,
    private val adminAuditRecorder: AdminAuditRecorder,
) {

    fun updateStatus(adminId: Long, recordId: Long, status: SpotRecordStatus) {
        val action = when (status) {
            SpotRecordStatus.HIDDEN -> {
                spotRecordModerationService.hide(recordId)
                AdminAuditAction.SPOT_RECORD_HIDE
            }
            SpotRecordStatus.PUBLISHED -> {
                spotRecordModerationService.restore(recordId)
                AdminAuditAction.SPOT_RECORD_RESTORE
            }
            SpotRecordStatus.DRAFT -> throw SpotRecordInvalidStatusException()
        }
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = action,
                targetType = AdminAuditTargetType.SPOT_RECORD,
                targetId = recordId,
            ),
        )
    }
}
