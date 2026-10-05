package com.peakda.server.domain.notification.repository

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

class BloomTimingAlertRepositoryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var repository: BloomTimingAlertRepository

    @BeforeEach
    fun cleanUp() {
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `동일 발송 키는 최초 한 번만 삽입한다`() {
        val peakStartDate = LocalDate.of(2026, 4, 8)

        val first = insert(peakYear = 2026, peakStartDate = peakStartDate)
        val duplicate = insert(peakYear = 2026, peakStartDate = peakStartDate.plusDays(1))

        assertThat(first).isEqualTo(1)
        assertThat(duplicate).isZero()
        assertThat(repository.findAll()).hasSize(1)
        assertThat(repository.findAll().single().peakStartDate).isEqualTo(peakStartDate)
    }

    @Test
    @Transactional
    fun `만개 연도가 다르면 별도 행으로 삽입한다`() {
        val first = insert(peakYear = 2026, peakStartDate = LocalDate.of(2026, 4, 8))
        val nextYear = insert(peakYear = 2027, peakStartDate = LocalDate.of(2027, 4, 8))

        assertThat(first).isEqualTo(1)
        assertThat(nextYear).isEqualTo(1)
        assertThat(repository.findAll()).hasSize(2)
    }

    @Test
    @Transactional
    fun `사용자 id로 발송 로그를 삭제한다`() {
        insert(userId = 1L, peakYear = 2026, peakStartDate = LocalDate.of(2026, 4, 8))
        insert(userId = 2L, peakYear = 2026, peakStartDate = LocalDate.of(2026, 4, 8))

        repository.deleteByUserId(1L)

        assertThat(repository.findAll()).extracting<Long> { it.userId }.containsExactly(2L)
    }

    private fun insert(
        userId: Long = 1L,
        peakYear: Int,
        peakStartDate: LocalDate,
    ): Int = repository.insertIfAbsent(
        userId = userId,
        spotId = 10L,
        bloomCategory = BloomCategory.CHERRY.name,
        peakYear = peakYear,
        peakStartDate = peakStartDate,
    )
}
