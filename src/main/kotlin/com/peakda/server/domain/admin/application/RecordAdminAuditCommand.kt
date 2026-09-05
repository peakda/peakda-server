package com.peakda.server.domain.admin.application

import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType

data class RecordAdminAuditCommand(
    val adminId: Long,
    val action: AdminAuditAction,
    val targetType: AdminAuditTargetType,
    val targetId: Long,
    val memo: String? = null,
)
