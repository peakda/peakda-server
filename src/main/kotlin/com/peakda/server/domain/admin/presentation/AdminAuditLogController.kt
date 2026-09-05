package com.peakda.server.domain.admin.presentation

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.common.response.ApiResponse
import com.peakda.server.domain.admin.application.AdminAuditQueryService
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.admin.presentation.response.AdminAuditLogResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/audit-logs")
class AdminAuditLogController(
    private val adminAuditQueryService: AdminAuditQueryService,
) : AdminAuditLogControllerDocs {

    override fun list(
        targetType: AdminAuditTargetType?,
        targetId: Long?,
        adminId: Long?,
        pageRequest: PageRequest,
    ): ResponseEntity<ApiResponse<PageResponse<AdminAuditLogResponse>>> {
        val response = adminAuditQueryService
            .list(targetType, targetId, adminId, pageRequest.toPageable())
            .toPageResponse()
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, response))
    }
}
