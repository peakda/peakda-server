package com.peakda.server.domain.admin.application

import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.admin.presentation.response.AdminAuditLogResponse
import com.peakda.server.domain.admin.repository.AdminAuditLogRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminAuditQueryService(
    private val adminAuditLogRepository: AdminAuditLogRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun list(
        targetType: AdminAuditTargetType?,
        targetId: Long?,
        adminId: Long?,
        pageable: Pageable,
    ): Page<AdminAuditLogResponse> {
        val auditLogs = adminAuditLogRepository.search(targetType, targetId, adminId, pageable)
        val adminNicknames = userRepository.findAllById(auditLogs.content.map { it.adminId }.distinct())
            .associate { requireNotNull(it.id) to it.nickname }

        return auditLogs.map { auditLog ->
            AdminAuditLogResponse.from(auditLog, adminNicknames[auditLog.adminId])
        }
    }
}
