package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.Region
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

class SpotBloomMapServiceTest {

    private val attractionRepository = mock(AttractionRepository::class.java)
    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)
    private val spotRepository = mock(SpotRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordPlantRepository = mock(SpotRecordPlantRepository::class.java)
    private val plantRepository = mock(PlantRepository::class.java)

    private val service = SpotBloomMapService(
        attractionRepository,
        seasonalBloomEstimateRepository,
        spotRepository,
        spotRecordRepository,
        spotRecordPlantRepository,
        plantRepository,
    )

    private val baseDate = LocalDate.of(2026, 3, 30)

    @Test
    fun `명소형 핀은 추정을 상속하고 기존 Spot 행이 있으면 spotId 를 채우며 ENDED 슬롯은 제외한다`() {
        stubAttractions(attraction(ATTRACTION_ID, "남산"))
        stubAttractionEstimates(
            estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.9),
            estimate(BloomCategory.AZALEA, BloomStatus.ENDED, confidence = 0.8),
        )
        stubMaterializedSpot(spotId = 100L)
        stubNoLocalSpots()

        val response = service.map(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG, category = null, date = null)

        assertThat(response.baseDate).isEqualTo(baseDate)
        assertThat(response.pins).hasSize(1)
        val pin = response.pins.first()
        assertThat(pin.spotId).isEqualTo(100L)
        assertThat(pin.attractionId).isEqualTo(ATTRACTION_ID)
        assertThat(pin.type).isEqualTo(SpotType.ATTRACTION)
        assertThat(pin.blooms).extracting<BloomCategory> { it.category }.containsExactly(BloomCategory.CHERRY)

        // 하위호환 alias: 명소형 핀이 옛 구조(attractions)로도 제공된다.
        assertThat(response.attractions).hasSize(1)
        assertThat(response.attractions.first().attractionId).isEqualTo(ATTRACTION_ID)
        assertThat(response.attractions.first().title).isEqualTo("남산")
    }

    @Test
    fun `명소형 핀은 materialize 된 Spot 행이 없으면 spotId 가 null 이다`() {
        stubAttractions(attraction(ATTRACTION_ID, "남산"))
        stubAttractionEstimates(estimate(BloomCategory.CHERRY, BloomStatus.STARTED, confidence = 0.7))
        `when`(spotRepository.findByTypeAndAttractionIdIn(SpotType.ATTRACTION, listOf(ATTRACTION_ID)))
            .thenReturn(emptyList())
        stubNoLocalSpots()

        val response = service.map(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG, category = null, date = null)

        assertThat(response.pins).hasSize(1)
        assertThat(response.pins.first().spotId).isNull()
        assertThat(response.pins.first().attractionId).isEqualTo(ATTRACTION_ID)
    }

    @Test
    fun `동네형 핀은 최근 기록의 단계를 상태로 환산하고 LATE 는 제외한다`() {
        stubNoAttractions()
        val spot = localSpot(SPOT_ID, "벚꽃길")
        `when`(spotRepository.findVisibleInBoundingBox(SpotType.LOCAL, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG))
            .thenReturn(listOf(spot))

        // CHERRY: 최근(rec1 PEAK) 이 과거(rec2 EARLY) 보다 우선. CAMELLIA: 유일 기록이 LATE → 제외.
        val rec1 = record(1L, SPOT_ID, LocalDate.of(2026, 4, 1), BloomStage.PEAK)
        val rec2 = record(2L, SPOT_ID, LocalDate.of(2026, 3, 20), BloomStage.EARLY)
        val rec3 = record(3L, SPOT_ID, LocalDate.of(2026, 4, 2), BloomStage.LATE)
        `when`(spotRecordRepository.findBySpotIdInAndStatus(listOf(SPOT_ID), SpotRecordStatus.PUBLISHED))
            .thenReturn(listOf(rec1, rec2, rec3))

        val cherry = plant(10L, BloomCategory.CHERRY)
        val camellia = plant(11L, BloomCategory.CAMELLIA)
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(listOf(1L, 2L, 3L))).thenReturn(
            listOf(
                SpotRecordPlant(SpotRecordPlantId(1L, 10L)),
                SpotRecordPlant(SpotRecordPlantId(2L, 10L)),
                SpotRecordPlant(SpotRecordPlantId(3L, 11L)),
            ),
        )
        `when`(plantRepository.findAllById(setOf(10L, 11L))).thenReturn(listOf(cherry, camellia))

        val response = service.map(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG, category = null, date = null)

        assertThat(response.pins).hasSize(1)
        val pin = response.pins.first()
        assertThat(pin.spotId).isEqualTo(SPOT_ID)
        assertThat(pin.type).isEqualTo(SpotType.LOCAL)
        assertThat(pin.attractionId).isNull()
        assertThat(pin.blooms).hasSize(1)
        assertThat(pin.blooms.first().category).isEqualTo(BloomCategory.CHERRY)
        assertThat(pin.blooms.first().status).isEqualTo(BloomStatus.PEAK)
        // 동네형은 하위호환 alias(attractions)에 포함되지 않는다.
        assertThat(response.attractions).isEmpty()
    }

    @Test
    fun `방문예정일이 주어지면 명소형 상태를 절정 구간 기준으로 재계산한다`() {
        stubAttractions(attraction(ATTRACTION_ID, "여좌천"))
        // 저장 상태는 PREPARING 이지만 방문예정일이 절정 구간 안이면 PEAK 로 재계산된다.
        stubAttractionEstimates(
            estimate(
                BloomCategory.CHERRY,
                BloomStatus.PREPARING,
                confidence = 0.8,
                peakStart = LocalDate.of(2026, 4, 1),
                peakEnd = LocalDate.of(2026, 4, 10),
            ),
        )
        stubMaterializedSpot(spotId = 100L)
        stubNoLocalSpots()

        val response = service.map(
            MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG,
            category = null,
            date = LocalDate.of(2026, 4, 5),
        )

        assertThat(response.pins.first().blooms.first().status).isEqualTo(BloomStatus.PEAK)
    }

    @Test
    fun `category 필터는 해당 카테고리 슬롯만 남긴다`() {
        stubAttractions(attraction(ATTRACTION_ID, "남산"))
        `when`(
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdInAndBloomCategory(
                baseDate, listOf(ATTRACTION_ID), BloomCategory.CHERRY,
            ),
        ).thenReturn(listOf(estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.9)))
        stubMaterializedSpot(spotId = 100L)
        stubNoLocalSpots()

        val response = service.map(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG, category = BloomCategory.CHERRY, date = null)

        assertThat(response.pins.first().blooms).extracting<BloomCategory> { it.category }
            .containsExactly(BloomCategory.CHERRY)
    }

    @Test
    fun `categories 반복 파라미터는 레포지토리 IN 조회로 합집합 슬롯을 반환한다`() {
        stubAttractions(attraction(ATTRACTION_ID, "남산"))
        `when`(
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdInAndBloomCategoryIn(
                baseDate,
                listOf(ATTRACTION_ID),
                listOf(BloomCategory.CHERRY, BloomCategory.AZALEA),
            ),
        ).thenReturn(
            listOf(
                estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.9),
                estimate(BloomCategory.AZALEA, BloomStatus.STARTED, confidence = 0.8),
            ),
        )
        stubMaterializedSpot(spotId = 100L)
        stubNoLocalSpots()

        val response = service.map(
            MIN_LAT,
            MAX_LAT,
            MIN_LNG,
            MAX_LNG,
            categories = listOf(BloomCategory.CHERRY, BloomCategory.AZALEA),
            status = null,
            region = null,
            date = null,
        )

        assertThat(response.pins.single().blooms).extracting<BloomCategory> { it.category }
            .containsExactlyInAnyOrder(BloomCategory.CHERRY, BloomCategory.AZALEA)
    }

    @Test
    fun `status 필터는 명소 추정과 동네 관측에 동일한 BloomStatus를 적용한다`() {
        val attraction = attraction(ATTRACTION_ID, "남산").also { it.areaCode = "1" }
        stubAttractions(attraction)
        stubAttractionEstimates(estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.9))
        stubMaterializedSpot(spotId = 100L)

        val local = localSpot(SPOT_ID, "벚꽃길").also { it.address = "서울특별시 중구" }
        `when`(spotRepository.findVisibleInBoundingBox(SpotType.LOCAL, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG))
            .thenReturn(listOf(local))
        val record = record(1L, SPOT_ID, LocalDate.of(2026, 4, 1), BloomStage.PEAK)
        `when`(spotRecordRepository.findBySpotIdInAndStatus(listOf(SPOT_ID), SpotRecordStatus.PUBLISHED))
            .thenReturn(listOf(record))
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(listOf(1L))).thenReturn(
            listOf(SpotRecordPlant(SpotRecordPlantId(1L, 10L))),
        )
        `when`(plantRepository.findAllById(setOf(10L))).thenReturn(listOf(plant(10L, BloomCategory.CHERRY)))

        val response = service.map(
            MIN_LAT,
            MAX_LAT,
            MIN_LNG,
            MAX_LNG,
            categories = null,
            status = BloomStatus.PEAK,
            region = Region.CAPITAL,
            date = null,
        )

        assertThat(response.pins).hasSize(2)
        assertThat(response.pins).allSatisfy { pin ->
            assertThat(pin.blooms).allMatch { it.status == BloomStatus.PEAK }
        }
    }

    @Test
    fun `권역 필터는 bbox 결과와 AND로 동작하고 LOCAL 판정 불가 주소는 제외한다`() {
        val attraction = attraction(ATTRACTION_ID, "세종 명소").also { it.areaCode = "8" }
        stubAttractions(attraction)
        stubAttractionEstimates(estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.9))
        stubMaterializedSpot(spotId = 100L)

        val unknownLocal = localSpot(SPOT_ID, "주소 불명")
        `when`(spotRepository.findVisibleInBoundingBox(SpotType.LOCAL, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG))
            .thenReturn(listOf(unknownLocal))

        val response = service.map(
            MIN_LAT,
            MAX_LAT,
            MIN_LNG,
            MAX_LNG,
            categories = null,
            status = null,
            region = Region.CHUNGCHEONG,
            date = null,
        )

        assertThat(response.pins).hasSize(1)
        assertThat(response.pins.single().attractionId).isEqualTo(ATTRACTION_ID)
    }

    // --- fixtures ---

    private fun stubAttractions(vararg attractions: Attraction) {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(attractionRepository.findVisibleInBoundingBox(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG))
            .thenReturn(attractions.toList())
    }

    private fun stubNoAttractions() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(attractionRepository.findVisibleInBoundingBox(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG))
            .thenReturn(emptyList())
    }

    private fun stubAttractionEstimates(vararg estimates: SeasonalBloomEstimate) {
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, listOf(ATTRACTION_ID)))
            .thenReturn(estimates.toList())
    }

    private fun stubMaterializedSpot(spotId: Long) {
        val spot = localSpot(spotId, "명소형스팟").also {
            ReflectionTestUtils.setField(it, "type", SpotType.ATTRACTION)
            ReflectionTestUtils.setField(it, "attractionId", ATTRACTION_ID)
        }
        `when`(spotRepository.findByTypeAndAttractionIdIn(SpotType.ATTRACTION, listOf(ATTRACTION_ID)))
            .thenReturn(listOf(spot))
    }

    private fun stubNoLocalSpots() {
        `when`(spotRepository.findVisibleInBoundingBox(SpotType.LOCAL, MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG))
            .thenReturn(emptyList())
    }

    private fun attraction(id: Long, title: String): Attraction {
        val attraction = Attraction(
            tourApiContentId = "c-$id",
            title = title,
            latitude = 37.55,
            longitude = 126.98,
        )
        ReflectionTestUtils.setField(attraction, "id", id)
        return attraction
    }

    private fun localSpot(id: Long, name: String): Spot {
        val spot = Spot(
            type = SpotType.LOCAL,
            name = name,
            latitude = 37.56,
            longitude = 126.99,
        )
        ReflectionTestUtils.setField(spot, "id", id)
        return spot
    }

    private fun estimate(
        category: BloomCategory,
        status: BloomStatus,
        confidence: Double,
        peakStart: LocalDate? = null,
        peakEnd: LocalDate? = null,
    ) = SeasonalBloomEstimate(
        attractionId = ATTRACTION_ID,
        bloomCategory = category,
        baseDate = baseDate,
        status = status,
        confidence = confidence,
        chosenEstimator = Estimator.CALENDAR,
        peakStartDate = peakStart,
        peakEndDate = peakEnd,
    )

    private fun record(id: Long, spotId: Long, visitedDate: LocalDate, stage: BloomStage): SpotRecord {
        val record = SpotRecord(
            spotId = spotId,
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
        private const val MIN_LAT = 37.4
        private const val MAX_LAT = 37.7
        private const val MIN_LNG = 126.8
        private const val MAX_LNG = 127.1
    }
}
