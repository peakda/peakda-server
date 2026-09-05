package com.peakda.server.domain.admin.application

import com.peakda.server.domain.admin.entity.AdminAuditLog
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.admin.repository.AdminAuditLogRepository
import com.peakda.server.domain.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class AdminAuditQueryServiceTest {

    private val repository = mock(AdminAuditLogRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val service = AdminAuditQueryService(repository, userRepository)
    private val pageable = PageRequest.of(0, 20)

    @Test
    fun `대상 타입만 지정해도 검색 조건으로 전달한다`() {
        `when`(
            repository.search(AdminAuditTargetType.SPOT_RECORD, null, null, pageable),
        ).thenReturn(PageImpl(emptyList<AdminAuditLog>(), pageable, 0))

        service.list(AdminAuditTargetType.SPOT_RECORD, null, null, pageable)

        verify(repository).search(AdminAuditTargetType.SPOT_RECORD, null, null, pageable)
    }

    @Test
    fun `대상과 관리자 필터를 모두 지정하면 세 조건을 함께 전달한다`() {
        `when`(
            repository.search(AdminAuditTargetType.SPOT_RECORD, TARGET_ID, ADMIN_ID, pageable),
        ).thenReturn(PageImpl(emptyList<AdminAuditLog>(), pageable, 0))

        service.list(AdminAuditTargetType.SPOT_RECORD, TARGET_ID, ADMIN_ID, pageable)

        verify(repository).search(AdminAuditTargetType.SPOT_RECORD, TARGET_ID, ADMIN_ID, pageable)
    }

    companion object {
        private const val TARGET_ID = 1024L
        private const val ADMIN_ID = 7L
    }
}
