package com.peakda.server.domain.launchalert.repository

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.launchalert.entity.LaunchAlertDispatch
import com.peakda.server.domain.launchalert.entity.LaunchAlertPlatform
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** upsert 의 ON CONFLICT, 커서 조회, 발송 중 한 건 제약이 PostgreSQL 에서 실제로 맞게 도는지 확인한다. */
class LaunchAlertSubscriptionRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var repository: LaunchAlertSubscriptionRepository

    @Autowired
    lateinit var dispatchRepository: LaunchAlertDispatchRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
        dispatchRepository.deleteAll()
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
    fun `커서 다음 신청만 id 순으로 가져오고 보낸 신청은 한 건씩 지운다`() {
        listOf("a", "b", "c").forEach { repository.upsert(IOS, "$it@peakda.com", "v1", FIRST) }
        val ids = repository.findAll().map { requireNotNull(it.id) }.sorted()

        val next = repository.findTop100ByPlatformAndIdGreaterThanOrderByIdAsc(LaunchAlertPlatform.IOS, ids[0])
        val deleted = repository.deleteSent(ids[1])
        entityManager.clear()

        assertThat(next.map { it.email }).containsExactly("b@peakda.com", "c@peakda.com")
        assertThat(deleted).isEqualTo(1)
        assertThat(repository.countByPlatform(LaunchAlertPlatform.IOS)).isEqualTo(2)
    }

    @Test
    fun `플랫폼마다 발송 중인 발송은 하나뿐이다`() {
        dispatchRepository.saveAndFlush(dispatch())

        assertThatThrownBy { dispatchRepository.saveAndFlush(dispatch()) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun dispatch() =
        LaunchAlertDispatch(platform = LaunchAlertPlatform.IOS, storeUrl = "https://apps.apple.com/kr/app/id1", requestedBy = 1L)

    companion object {
        private val IOS = LaunchAlertPlatform.IOS.name
        private val FIRST = Instant.parse("2026-10-21T00:00:00Z")
        private val SECOND = Instant.parse("2026-10-22T00:00:00Z")
    }
}
