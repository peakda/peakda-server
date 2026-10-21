package com.peakda.server.domain.launchalert.repository

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** upsert 의 ON CONFLICT 와 파기 범위가 PostgreSQL 에서 실제로 맞게 도는지 확인한다. */
class LaunchAlertSubscriptionRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var repository: LaunchAlertSubscriptionRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `같은 이메일을 다시 신청하면 행을 늘리지 않고 동의 정보만 갱신한다`() {
        repository.upsert(IOS, "a@peakda.com", "v1", FIRST)
        repository.upsert(IOS, "a@peakda.com", "v2", SECOND)
        entityManager.clear()

        val subscription = repository.findAll().single()
        assertThat(subscription.consentVersion).isEqualTo("v2")
        assertThat(subscription.consentedAt).isEqualTo(SECOND)
        assertThat(subscription.createdAt).isEqualTo(FIRST)
    }

    @Test
    @Transactional
    fun `파기는 내려받은 신청만 지워 그 뒤 신청은 남긴다`() {
        repository.upsert(IOS, "a@peakda.com", "v1", FIRST)
        repository.upsert(IOS, "b@peakda.com", "v1", FIRST)
        val exported = repository.findAllByPlatformOrderByIdAsc(LaunchAlertPlatform.IOS)
        repository.upsert(IOS, "c@peakda.com", "v1", SECOND)

        val marked = repository.markExported(exported.map { requireNotNull(it.id) }.toLongArray(), SECOND)
        val deleted = repository.deleteExported(LaunchAlertPlatform.IOS)
        entityManager.clear()

        assertThat(marked).isEqualTo(2)
        assertThat(deleted).isEqualTo(2)
        assertThat(repository.findAll().map { it.email }).containsExactly("c@peakda.com")
    }

    @Test
    @Transactional
    fun `다시 내려받아도 처음 내려받은 시각을 유지한다`() {
        repository.upsert(IOS, "a@peakda.com", "v1", FIRST)
        val id = requireNotNull(repository.findAll().single().id)

        repository.markExported(longArrayOf(id), FIRST)
        val markedAgain = repository.markExported(longArrayOf(id), SECOND)
        entityManager.clear()

        assertThat(markedAgain).isZero()
        assertThat(repository.findAll().single().exportedAt).isEqualTo(FIRST)
    }

    companion object {
        private val IOS = LaunchAlertPlatform.IOS.name
        private val FIRST = Instant.parse("2026-10-21T00:00:00Z")
        private val SECOND = Instant.parse("2026-10-22T00:00:00Z")
    }
}
