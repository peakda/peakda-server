package com.peakda.server.domain.seasonal.application.estimator

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.seasonal.application.BloomEstimationContext
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

class UserRecordBloomEstimatorTest {

    private val spotRepository = mock(SpotRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordPlantRepository = mock(SpotRecordPlantRepository::class.java)
    private val plantRepository = mock(PlantRepository::class.java)

    private val properties = UserRecordEstimatorProperties(
        enabled = true,
        baseConfidence = 0.75,
        minConfidence = 0.3,
        maxAgeDays = 14,
        lookbackRecords = 20,
    )

    private val estimator = UserRecordBloomEstimator(
        properties,
        spotRepository,
        spotRecordRepository,
        spotRecordPlantRepository,
        plantRepository,
    )

    private val baseDate = LocalDate.of(2026, 3, 30)

    @Test
    fun `비활성화 상태면 항상 null 이다`() {
        val disabled = UserRecordBloomEstimator(
            properties.copy(enabled = false),
            spotRepository,
            spotRecordRepository,
            spotRecordPlantRepository,
            plantRepository,
        )

        assertThat(disabled.estimate(context())).isNull()
    }

    @Test
    fun `명소형 Spot 이 아직 materialize 되지 않았으면 null 이다`() {
        `when`(spotRepository.findByTypeAndAttractionId(SpotType.ATTRACTION, ATTRACTION_ID)).thenReturn(null)

        assertThat(estimator.estimate(context())).isNull()
    }

    @Test
    fun `게시된 기록이 없으면 null 이다`() {
        stubMaterializedSpot()
        stubRecords(emptyList())

        assertThat(estimator.estimate(context())).isNull()
    }

    @Test
    fun `카테고리가 일치하는 기록이 없으면 null 이다`() {
        stubMaterializedSpot()
        val rec = record(1L, LocalDate.of(2026, 3, 28), BloomStage.PEAK)
        stubRecords(listOf(rec))
        stubCategories(listOf(1L to BloomCategory.AZALEA_KR))

        assertThat(estimator.estimate(context(category = BloomCategory.CHERRY))).isNull()
    }

    @Test
    fun `maxAgeDays 를 넘긴 기록은 null 이다`() {
        stubMaterializedSpot()
        val rec = record(1L, baseDate.minusDays(15), BloomStage.PEAK)
        stubRecords(listOf(rec))
        stubCategories(listOf(1L to BloomCategory.CHERRY))

        assertThat(estimator.estimate(context())).isNull()
    }

    @Test
    fun `가장 최근 일치 기록의 단계를 상태로 환산하고 경과일에 따라 신뢰도가 선형 감쇠한다`() {
        stubMaterializedSpot()
        val older = record(1L, baseDate.minusDays(7), BloomStage.EARLY)
        val newer = record(2L, baseDate.minusDays(7), BloomStage.PEAK)
        stubRecords(listOf(newer, older))
        stubCategories(listOf(1L to BloomCategory.CHERRY, 2L to BloomCategory.CHERRY))

        val estimation = estimator.estimate(context())

        assertThat(estimation).isNotNull
        assertThat(estimation!!.estimator).isEqualTo(Estimator.USER_RECORD)
        assertThat(estimation.status).isEqualTo(BloomStatus.PEAK)
        // ageDays=7 -> decayRatio=0.5 -> 0.75 - 0.5*(0.75-0.3) = 0.525
        assertThat(estimation.confidence).isEqualTo(0.525, org.assertj.core.data.Offset.offset(1e-9))
    }

    @Test
    fun `같은 카테고리 여러 기록 중 방문일이 더 최신인 쪽을 채택한다`() {
        stubMaterializedSpot()
        val older = record(1L, baseDate.minusDays(10), BloomStage.EARLY)
        val newer = record(2L, baseDate.minusDays(1), BloomStage.STARTING)
        stubRecords(listOf(older, newer))
        stubCategories(listOf(1L to BloomCategory.CHERRY, 2L to BloomCategory.CHERRY))

        val estimation = estimator.estimate(context())

        assertThat(estimation).isNotNull
        assertThat(estimation!!.status).isEqualTo(BloomStatus.STARTED)
        assertThat(estimation.evidence).isEqualTo("spot_record:2,stage:STARTING")
    }

    // --- fixtures ---

    private fun context(category: BloomCategory = BloomCategory.CHERRY) =
        BloomEstimationContext(attraction(), category, baseDate)

    private fun attraction(): Attraction {
        val attraction = Attraction(
            tourApiContentId = "c-$ATTRACTION_ID",
            title = "남산",
            latitude = 37.55,
            longitude = 126.98,
        )
        ReflectionTestUtils.setField(attraction, "id", ATTRACTION_ID)
        return attraction
    }

    private fun stubMaterializedSpot() {
        val spot = Spot(
            type = SpotType.ATTRACTION,
            attractionId = ATTRACTION_ID,
            name = "남산",
            latitude = 37.55,
            longitude = 126.98,
        )
        ReflectionTestUtils.setField(spot, "id", SPOT_ID)
        `when`(spotRepository.findByTypeAndAttractionId(SpotType.ATTRACTION, ATTRACTION_ID)).thenReturn(spot)
    }

    private fun stubRecords(records: List<SpotRecord>) {
        `when`(
            spotRecordRepository.findBySpotIdAndStatusOrderByCreatedAtDesc(
                SPOT_ID,
                SpotRecordStatus.PUBLISHED,
                PageRequest.of(0, properties.lookbackRecords),
            ),
        ).thenReturn(PageImpl(records))
    }

    private fun stubCategories(recordIdToCategory: List<Pair<Long, BloomCategory>>) {
        val recordIds = recordIdToCategory.map { it.first }
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(anyList())).thenReturn(
            recordIdToCategory.map { (recordId, _) -> SpotRecordPlant(SpotRecordPlantId(recordId, recordId)) },
        )
        val plants = recordIdToCategory.map { (recordId, category) -> plant(recordId, category) }
        `when`(plantRepository.findAllById(recordIds.toSet())).thenReturn(plants)
    }

    private fun record(id: Long, visitedDate: LocalDate, stage: BloomStage): SpotRecord {
        val record = SpotRecord(
            spotId = SPOT_ID,
            userId = 7L,
            visitedDate = visitedDate,
            bloomStage = stage,
            status = SpotRecordStatus.PUBLISHED,
        )
        ReflectionTestUtils.setField(record, "id", id)
        return record
    }

    private fun plant(id: Long, category: BloomCategory): Plant {
        val plant = Plant(name = "p-$id", status = PlantStatus.ACTIVE, bloomCategory = category)
        ReflectionTestUtils.setField(plant, "id", id)
        return plant
    }

    companion object {
        private const val ATTRACTION_ID = 501L
        private const val SPOT_ID = 100L
    }
}
