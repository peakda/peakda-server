package com.peakda.server.domain.admin.presentation.response

import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditLog
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "관리자 조치 감사 로그")
data class AdminAuditLogResponse(
    @field:Schema(description = "감사 로그 id", example = "1001")
    val id: Long,

    @field:Schema(description = "조치를 수행한 관리자 사용자 id", example = "7")
    val adminId: Long,

    @field:Schema(description = "조치를 수행한 관리자 닉네임", example = "운영자", nullable = true)
    val adminNickname: String?,

    @field:Schema(description = "관리자 조치 종류", example = "CURATION_UPSERT")
    val action: AdminAuditAction,

    @field:Schema(description = "조치 대상 종류", example = "CURATION")
    val targetType: AdminAuditTargetType,

    @field:Schema(description = "조치 대상 id", example = "101")
    val targetId: Long,

    @field:Schema(description = "조치 사유 또는 부가 정보", example = "발행 상태로 수정", nullable = true)
    val memo: String?,

    @field:Schema(description = "조치 시각", example = "2026-07-28T09:30:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(auditLog: AdminAuditLog, adminNickname: String?): AdminAuditLogResponse =
            AdminAuditLogResponse(
                id = requireNotNull(auditLog.id),
                adminId = auditLog.adminId,
                adminNickname = adminNickname,
                action = auditLog.action,
                targetType = auditLog.targetType,
                targetId = auditLog.targetId,
                memo = auditLog.memo,
                createdAt = auditLog.createdAt,
            )
    }
}
