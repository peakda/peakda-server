package com.peakda.server.common.test

import com.peakda.server.domain.auth.application.RefreshTokenService
import org.redisson.api.RedissonClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.PostgreSQLContainer

/**
 * PostgreSQL 이 필요한 통합 테스트의 공용 베이스.
 *
 * 컨테이너는 JVM 당 한 번만 띄우고 모든 테스트 클래스가 같이 쓴다. 클래스마다 `@Container` 를 두면
 * 컨테이너가 클래스마다 새로 떠 Spring 컨텍스트도 클래스 수만큼 생기고, 캐시에 남은 컨텍스트가
 * 이미 내려간 컨테이너에 재접속을 시도하며 메모리를 잡아먹는다.
 *
 * 하위 클래스가 `@MockitoBean` 을 더 선언하거나 설정을 바꾸면 컨텍스트가 따로 생기므로, 꼭 필요할 때만 추가한다.
 * DB 를 공유하므로 테스트는 `@Transactional` 로 롤백하고, 데이터를 커밋하는 테스트는 `@BeforeEach` 에서
 * 자기가 쓰는 테이블을 비운다. 클래스 전체가 커밋하는 경우는 `@AfterEach` 로도 비워 뒤 클래스에 남기지 않는다.
 * (`@Transactional` 테스트의 `@AfterEach` 는 같은 테스트 트랜잭션 안에서 돌아, 제약 위반을 일부러 낸 테스트에서 실패한다.)
 */
@SpringBootTest
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    @MockitoBean
    lateinit var refreshTokenService: RefreshTokenService

    @MockitoBean
    lateinit var redissonClient: RedissonClient

    companion object {
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
            .withDatabaseName("peakda")
            .withUsername("peakda")
            .withPassword("peakda")
            .apply { start() }
    }
}
