package com.peakda.server.domain.user.application

import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.exception.AdminUserNotFoundException
import com.peakda.server.domain.user.exception.AdminSelfActionNotAllowedException
import com.peakda.server.domain.user.exception.UserStatusNotChangeableException
import com.peakda.server.domain.user.presentation.response.UserAdminResponse
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserAdminService(
    private val userRepository: UserRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
) {

    @Transactional(readOnly = true)
    fun list(q: String?, status: UserStatus?, role: UserRole?, pageable: Pageable): Page<UserAdminResponse> =
        userRepository.findAdminUsers(nicknamePatternOf(q), status, role, pageable)
            .map(UserAdminResponse::from)

    /**
     * 닉네임 부분일치 LIKE 패턴을 만든다. 검색어가 없으면 `%` 를 돌려 전체를 매칭한다.
     * 레포지토리에 null 을 넘기면 PostgreSQL 이 파라미터 타입을 정하지 못해 실패하므로 항상 값이 있어야 한다.
     * 사용자가 넣은 `%`·`_`·`\` 는 와일드카드로 해석되지 않도록 이스케이프한다.
     */
    private fun nicknamePatternOf(q: String?): String {
        val keyword = q?.trim().orEmpty()
        if (keyword.isEmpty()) return "%"
        val escaped = keyword
            .replace("""\""", """\\""")
            .replace("%", """\%""")
            .replace("_", """\_""")
        return "%$escaped%"
    }

    fun changeStatus(adminId: Long, userId: Long, command: ChangeUserStatusCommand): UserAdminResponse {
        if (adminId == userId) throw AdminSelfActionNotAllowedException()

        val user = userRepository.findByIdForUpdate(userId) ?: throw AdminUserNotFoundException()
        val action = when (user.status to command.status) {
            UserStatus.ACTIVE to UserStatus.SUSPENDED -> AdminAuditAction.USER_SUSPEND
            UserStatus.SUSPENDED to UserStatus.ACTIVE -> AdminAuditAction.USER_UNSUSPEND
            else -> throw UserStatusNotChangeableException()
        }

        user.status = command.status
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = action,
                targetType = AdminAuditTargetType.USER,
                targetId = userId,
                memo = command.memo,
            ),
        )
        return UserAdminResponse.from(user)
    }
}
