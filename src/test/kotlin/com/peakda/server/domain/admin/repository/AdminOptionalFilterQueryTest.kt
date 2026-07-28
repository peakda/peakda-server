package com.peakda.server.domain.admin.repository

import com.peakda.server.domain.auth.application.RefreshTokenService
import com.peakda.server.domain.user.entity.UserRole
import com.peakda.server.domain.user.entity.UserStatus
import com.peakda.server.domain.user.repository.UserRepository
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobRunRepository
import com.peakda.server.infrastructure.scheduler.history.SchedulerJobStatus
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant

/**
 * 백오피스 목록 API 의 선택 필터가 PostgreSQL 에서 실제로 실행되는지 검증한다.
 *
 * 이 쿼리들을 `(:param IS NULL OR ...)` 로 작성하면 Hibernate 가 같은 이름의 파라미터를
 * 두 개의 placeholder 로 펴는데, `IS NULL` 쪽은 타입을 유추할 문맥이 없어 PostgreSQL 이
 * `could not determine data type of parameter $N` (SQLState 42P18) 으로 거부한다.
 * 문자열을 `LOWER()` 에 null 로 넣는 경우도 `function lower(bytea) does not exist` 로 실패한다.
 *
 * 두 실패 모두 컴파일과 mock 기반 단위 테스트로는 잡히지 않고 런타임 500 으로만 드러나므로,
 * 실제 컨테이너에 붙여 **모든 필터가 비어 있는** 최악의 조합을 실행해 둔다.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AdminOptionalFilterQueryTest {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    @Autowired
    lateinit var adminAuditLogRepository: AdminAuditLogRepository

    @Autowired
    lateinit var schedulerJobRunRepository: SchedulerJobRunRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `감사 로그 조회는 세 필터가 모두 비어도 실행된다`() {
        assertThatCode {
            adminAuditLogRepository.search(
                targetType = null,
                targetId = null,
                adminId = null,
                pageable = PAGEABLE,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `잡 실행 이력 조회는 세 필터가 모두 비어도 실행된다`() {
        assertThatCode {
            schedulerJobRunRepository.findRuns(
                jobName = null,
                status = null,
                since = null,
                pageable = PAGEABLE,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `잡 실행 이력 조회는 세 필터를 모두 지정해도 실행된다`() {
        assertThatCode {
            schedulerJobRunRepository.findRuns(
                jobName = "bloomEstimate",
                status = SchedulerJobStatus.FAILED,
                since = Instant.now().minusSeconds(86_400),
                pageable = PAGEABLE,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `사용자 검색은 검색어와 필터가 모두 비어도 실행된다`() {
        assertThatCode {
            userRepository.findAdminUsers(
                nicknamePattern = "%",
                status = null,
                role = null,
                pageable = PAGEABLE,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `사용자 검색은 검색어와 필터를 모두 지정해도 실행된다`() {
        assertThatCode {
            userRepository.findAdminUsers(
                nicknamePattern = "%운영%",
                status = UserStatus.ACTIVE,
                role = UserRole.ADMIN,
                pageable = PAGEABLE,
            )
        }.doesNotThrowAnyException()
    }

    companion object {
        private val PAGEABLE = PageRequest.of(0, 20)

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
    }
}
