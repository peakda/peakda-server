package com.peakda.server.domain.admin.application

import com.peakda.server.domain.admin.entity.AdminAuditLog
import com.peakda.server.domain.admin.repository.AdminAuditLogRepository
import org.springframework.stereotype.Component

@Component
class AdminAuditRecorder(
    private val repository: AdminAuditLogRepository,
) {
    fun record(command: RecordAdminAuditCommand) {
        repository.save(
            AdminAuditLog(
                adminId = command.adminId,
                action = command.action,
                targetType = command.targetType,
                targetId = command.targetId,
                memo = command.memo,
            ),
        )
    }
}
