package com.peakda.server.domain.spot.repository

import com.peakda.server.common.test.IntegrationTestSupport
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class SpotSitemapQueryTest : IntegrationTestSupport() {

    @Autowired
    lateinit var spotRepository: SpotRepository

    @Autowired
    lateinit var spotRecordRepository: SpotRecordRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @BeforeEach
    fun cleanUp() {
        spotRecordRepository.deleteAll()
        spotRepository.deleteAll()
    }

    @Test
    fun `sitemap 스팟 조회는 해당 유형의 공개 스팟만 id 오름차순으로 돌려준다`() {
        val visible = saveSpot(SpotType.ATTRACTION, attractionId = 1L, visible = true)
        saveSpot(SpotType.ATTRACTION, attractionId = 2L, visible = false)
        saveSpot(SpotType.LOCAL, attractionId = null, visible = true)

        val rows = spotRepository.findByTypeAndVisibleTrueOrderByIdAsc(SpotType.ATTRACTION)

        assertThat(rows.map { it.id }).containsExactly(visible.id)
        assertThat(rows.single().updatedAt).isNotNull()
    }

    @Test
    fun `스팟별 마지막 기록 수정 시각은 요청 상태의 기록만 집계한다`() {
        saveRecord(SPOT_ID, SpotRecordStatus.PUBLISHED)
        val latest = saveRecord(SPOT_ID, SpotRecordStatus.PUBLISHED)
        saveRecord(OTHER_SPOT_ID, SpotRecordStatus.DRAFT)
        entityManager.flush()
        entityManager.clear()

        val rows = spotRecordRepository.findLastModifiedAtPerSpotByStatus(SpotRecordStatus.PUBLISHED)

        val latestUpdatedAt = spotRecordRepository.findById(requireNotNull(latest.id)).orElseThrow().updatedAt
        assertThat(rows.map { it.spotId }).containsExactly(SPOT_ID)
        assertThat(rows.single().lastModifiedAt).isEqualTo(latestUpdatedAt)
    }

    private fun saveSpot(type: SpotType, attractionId: Long?, visible: Boolean): Spot = spotRepository.saveAndFlush(
        Spot(
            type = type,
            attractionId = attractionId,
            name = "스팟",
            latitude = 37.5,
            longitude = 127.0,
            visible = visible,
        ),
    )

    private fun saveRecord(spotId: Long, status: SpotRecordStatus): SpotRecord = spotRecordRepository.saveAndFlush(
        SpotRecord(spotId = spotId, userId = 1L, status = status),
    )

    companion object {
        private const val SPOT_ID = 100L
        private const val OTHER_SPOT_ID = 200L
    }
}
