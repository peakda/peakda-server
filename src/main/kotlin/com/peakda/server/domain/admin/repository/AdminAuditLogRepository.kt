package com.peakda.server.domain.admin.repository

import com.peakda.server.domain.admin.entity.AdminAuditLog
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AdminAuditLogRepository : JpaRepository<AdminAuditLog, Long> {
    /**
     * 대상 종류·대상 id·관리자 id 를 선택 조건으로 감사 로그를 조회한다. 세 조건은 서로 독립적으로 AND 결합된다.
     *
     * 선택 조건을 `(:param IS NULL OR ...)` 로 쓰면 PostgreSQL 이 실패한다.
     * Hibernate 가 같은 이름의 파라미터를 두 개의 placeholder 로 펴는데,
     * `IS NULL` 쪽 placeholder 는 타입을 유추할 문맥이 없어
     * `could not determine data type of parameter $N` (SQLState 42P18) 이 난다.
     * 그래서 파라미터를 NOT NULL 컬럼과 함께 `COALESCE` 에 넣어 컬럼에서 타입을 가져오게 한다.
     * `target_type`·`target_id`·`admin_id` 는 모두 NOT NULL 이므로 값이 없으면 조건이 항상 참이 된다.
     */
    @Query(
        """
            SELECT l FROM AdminAuditLog l
            WHERE l.targetType = COALESCE(:targetType, l.targetType)
              AND l.targetId = COALESCE(:targetId, l.targetId)
              AND l.adminId = COALESCE(:adminId, l.adminId)
            ORDER BY l.id DESC
        """,
    )
    fun search(
        @Param("targetType") targetType: AdminAuditTargetType?,
        @Param("targetId") targetId: Long?,
        @Param("adminId") adminId: Long?,
        pageable: Pageable,
    ): Page<AdminAuditLog>
}
