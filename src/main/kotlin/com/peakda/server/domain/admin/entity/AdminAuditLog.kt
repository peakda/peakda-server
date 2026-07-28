package com.peakda.server.domain.admin.entity

import com.peakda.server.common.persistence.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "admin_audit_logs",
    indexes = [
        Index(name = "ix_admin_audit_logs_target", columnList = "target_type,target_id,created_at"),
        Index(name = "ix_admin_audit_logs_admin", columnList = "admin_id,created_at"),
    ],
)
class AdminAuditLog(
    @Column(name = "admin_id", nullable = false)
    val adminId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, columnDefinition = "TEXT")
    val action: AdminAuditAction,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, columnDefinition = "TEXT")
    val targetType: AdminAuditTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: Long,

    @Column(name = "memo", columnDefinition = "TEXT")
    val memo: String? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
