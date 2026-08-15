package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.BloomStage
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRecordCount
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

class SpotPreviewServiceTest {

    private val spotRepository = mock(SpotRepository::class.java)
    private val attractionRepository = mock(AttractionRepository::class.java)
    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordPlantRepository = mock(SpotRecordPlantRepository::class.java)
    private val plantRepository = mock(PlantRepository::class.java)
    private val spotRecordPhotoRepository = mock(SpotRecordPhotoRepository::class.java)
    private val spotRecordPhotoUploader = mock(SpotRecordPhotoUploader::class.java)
    private val spotFavoriteRepository = mock(SpotFavoriteRepository::class.java)

    private val service = SpotPreviewService(
        spotRepository,
        attractionRepository,
        seasonalBloomEstimateRepository,
        spotRecordRepository,
        spotRecordPlantRepository,
        plantRepository,
        spotRecordPhotoRepository,
        spotRecordPhotoUploader,
        spotFavoriteRepository,
    )

    private val baseDate = LocalDate.of(2026, 3, 30)

    @Test
    fun `명소형 스팟은 ENDED 를 제외한 가장 강한 추정을 뱃지로, 명소 대표사진을 썸네일로 채운다`() {
        val spot = attractionSpot(SPOT_ID, ATTRACTION_ID)
        `when`(spotRepository.findAllById(listOf(SPOT_ID))).thenReturn(listOf(spot))
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, listOf(ATTRACTION_ID)))
            .thenReturn(
                listOf(
                    estimate(BloomCategory.AZALEA_KR, BloomStatus.ENDED, confidence = 0.95),
                    estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.8),
                ),
            )
        `when`(attractionRepository.findAllById(listOf(ATTRACTION_ID)))
            .thenReturn(listOf(attraction(ATTRACTION_ID, "https://img/primary.jpg")))

        val response = service.preview(listOf(SPOT_ID), category = null, lat = null, lng = null)

        assertThat(response.items).hasSize(1)
        val item = response.items.first()
        assertThat(item.badge?.category).isEqualTo(BloomCategory.CHERRY)
        assertThat(item.badge?.status).isEqualTo(BloomStatus.PEAK)
        assertThat(item.thumbnailUrl).isEqualTo("https://img/primary.jpg")
        assertThat(item.distanceMeters).isNull()
    }

    @Test
    fun `명소형 스팟은 ENDED 를 제외한 모든 뱃지를 상태와 신뢰도 순으로 반환한다`() {
        val spot = attractionSpot(SPOT_ID, ATTRACTION_ID)
        `when`(spotRepository.findAllById(listOf(SPOT_ID))).thenReturn(listOf(spot))
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, listOf(ATTRACTION_ID)))
            .thenReturn(
                listOf(
                    estimate(BloomCategory.AZALEA_KR, BloomStatus.STARTED, confidence = 0.99),
                    estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.7, peakStart = baseDate, peakEnd = baseDate.plusDays(3)),
                    estimate(BloomCategory.PLUM, BloomStatus.ENDED, confidence = 1.0),
                ),
            )

        val item = service.preview(listOf(SPOT_ID), categories = null, status = null, lat = null, lng = null, userId = USER_ID)
            .items.single()

        assertThat(item.badges.map { it.category }).containsExactly(BloomCategory.CHERRY, BloomCategory.AZALEA_KR)
        assertThat(item.badge).isEqualTo(item.badges.first())
        assertThat(item.badges.first().peakDurationDays).isEqualTo(4)
    }

    @Test
    fun `status 필터 후 뱃지가 비면 스팟 자체를 제외한다`() {
        val spot = attractionSpot(SPOT_ID, ATTRACTION_ID)
        `when`(spotRepository.findAllById(listOf(SPOT_ID))).thenReturn(listOf(spot))
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, listOf(ATTRACTION_ID)))
            .thenReturn(listOf(estimate(BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.9)))

        val response = service.preview(
            listOf(SPOT_ID),
            categories = null,
            status = BloomStatus.PREPARING,
            lat = null,
            lng = null,
            userId = USER_ID,
        )

        assertThat(response.items).isEmpty()
    }

    @Test
    fun `사진은 스팟당 최대 4장이고 찜 상태와 기록수는 배치 결과로 채우며 요청 순서를 보존한다`() {
        val first = localSpot(SPOT_ID)
        val second = localSpot(SECOND_SPOT_ID)
        `when`(spotRepository.findAllById(listOf(SPOT_ID, SECOND_SPOT_ID))).thenReturn(listOf(second, first))
        `when`(spotRecordPhotoRepository.findRecentPhotosBySpotIds(listOf(SECOND_SPOT_ID, SPOT_ID), SpotRecordStatus.PUBLISHED.name, 4))
            .thenReturn((1..5).map { photo(SPOT_ID, "key-$it") })
        (1..5).forEach { `when`(spotRecordPhotoUploader.presignedUrlOf("key-$it")).thenReturn("https://rec/$it.jpg") }
        `when`(spotFavoriteRepository.findByUserIdAndSpotIdIn(USER_ID, listOf(SPOT_ID, SECOND_SPOT_ID)))
            .thenReturn(listOf(SpotFavorite(userId = USER_ID, spotId = SPOT_ID, notifyEnabled = true)))
        `when`(spotRecordRepository.countBySpotIdInAndStatus(listOf(SPOT_ID, SECOND_SPOT_ID), SpotRecordStatus.PUBLISHED))
            .thenReturn(listOf(recordCount(SPOT_ID, 5)))

        val response = service.preview(
            listOf(SPOT_ID, SECOND_SPOT_ID),
            categories = null,
            status = null,
            lat = null,
            lng = null,
            userId = USER_ID,
        )

        assertThat(response.items.map { it.spotId }).containsExactly(SPOT_ID, SECOND_SPOT_ID)
        assertThat(response.items.first().photoUrls).containsExactly("https://rec/1.jpg", "https://rec/2.jpg", "https://rec/3.jpg", "https://rec/4.jpg")
        assertThat(response.items.first().thumbnailUrl).isEqualTo("https://rec/1.jpg")
        assertThat(response.items.first().favorited).isTrue()
        assertThat(response.items.first().notifyEnabled).isTrue()
        assertThat(response.items.first().recordCount).isEqualTo(5)
        assertThat(response.items[1].favorited).isFalse()
    }

    @Test
    fun `동네형 스팟은 카테고리 매칭되는 최근 게시 기록을 뱃지로, 최신 기록 사진을 썸네일로 채운다`() {
        val spot = localSpot(SPOT_ID)
        `when`(spotRepository.findAllById(listOf(SPOT_ID))).thenReturn(listOf(spot))
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(null)

        val rec1 = record(1L, SPOT_ID, LocalDate.of(2026, 3, 20), BloomStage.EARLY)
        val rec2 = record(2L, SPOT_ID, LocalDate.of(2026, 3, 28), BloomStage.PEAK)
        `when`(spotRecordRepository.findBySpotIdInAndStatus(listOf(SPOT_ID), SpotRecordStatus.PUBLISHED))
            .thenReturn(listOf(rec1, rec2))
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(anyList())).thenReturn(
            listOf(
                SpotRecordPlant(SpotRecordPlantId(1L, 10L)),
                SpotRecordPlant(SpotRecordPlantId(2L, 10L)),
            ),
        )
        `when`(plantRepository.findAllById(setOf(10L))).thenReturn(listOf(plant(10L, BloomCategory.CHERRY)))
        `when`(spotRecordPhotoRepository.findRecentPhotosBySpotIds(listOf(SPOT_ID), SpotRecordStatus.PUBLISHED.name, 4))
            .thenReturn(listOf(photo(spotId = SPOT_ID, objectKey = "key-2")))
        `when`(spotRecordPhotoUploader.presignedUrlOf("key-2")).thenReturn("https://rec/2.jpg")

        val response = service.preview(listOf(SPOT_ID), category = null, lat = null, lng = null)

        val item = response.items.first()
        assertThat(item.badge?.category).isEqualTo(BloomCategory.CHERRY)
        assertThat(item.badge?.status).isEqualTo(BloomStatus.PEAK)
        assertThat(item.thumbnailUrl).isEqualTo("https://rec/2.jpg")
    }

    @Test
    fun `category 필터에 맞는 기록이 없으면 뱃지는 null 이다`() {
        val spot = localSpot(SPOT_ID)
        `when`(spotRepository.findAllById(listOf(SPOT_ID))).thenReturn(listOf(spot))
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(null)

        val rec = record(1L, SPOT_ID, LocalDate.of(2026, 3, 28), BloomStage.PEAK)
        `when`(spotRecordRepository.findBySpotIdInAndStatus(listOf(SPOT_ID), SpotRecordStatus.PUBLISHED))
            .thenReturn(listOf(rec))
        `when`(spotRecordPlantRepository.findByIdSpotRecordIdIn(anyList()))
            .thenReturn(listOf(SpotRecordPlant(SpotRecordPlantId(1L, 10L))))
        `when`(plantRepository.findAllById(setOf(10L))).thenReturn(listOf(plant(10L, BloomCategory.CHERRY)))
        `when`(spotRecordPhotoRepository.findBySpotRecordIdIn(listOf(1L))).thenReturn(emptyList())

        val response = service.preview(listOf(SPOT_ID), category = BloomCategory.AZALEA_KR, lat = null, lng = null)

        assertThat(response.items.first().badge).isNull()
    }

    @Test
    fun `좌표가 주어지면 거리를 계산하고, 존재하지 않는 스팟은 결과에서 제외한다`() {
        val spot = localSpot(SPOT_ID, latitude = 37.55, longitude = 126.98)
        `when`(spotRepository.findAllById(listOf(SPOT_ID, MISSING_SPOT_ID))).thenReturn(listOf(spot))
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(null)

        val response = service.preview(listOf(SPOT_ID, MISSING_SPOT_ID), category = null, lat = 37.55, lng = 126.98)

        assertThat(response.items).hasSize(1)
        assertThat(response.items.first().spotId).isEqualTo(SPOT_ID)
        assertThat(response.items.first().distanceMeters).isEqualTo(0.0, org.assertj.core.data.Offset.offset(1e-6))
    }

    // --- fixtures ---

    private fun attractionSpot(id: Long, attractionId: Long): Spot {
        val spot = Spot(
            type = SpotType.ATTRACTION,
            attractionId = attractionId,
            name = "남산",
            latitude = 37.55,
            longitude = 126.98,
        )
        ReflectionTestUtils.setField(spot, "id", id)
        return spot
    }

    private fun localSpot(id: Long, latitude: Double = 37.56, longitude: Double = 126.99): Spot {
        val spot = Spot(
            type = SpotType.LOCAL,
            name = "벚꽃길",
            latitude = latitude,
            longitude = longitude,
        )
        ReflectionTestUtils.setField(spot, "id", id)
        return spot
    }

    private fun attraction(id: Long, primaryImageUrl: String?): Attraction {
        val attraction = Attraction(
            tourApiContentId = "c-$id",
            title = "남산",
            latitude = 37.55,
            longitude = 126.98,
            primaryImageUrl = primaryImageUrl,
        )
        ReflectionTestUtils.setField(attraction, "id", id)
        return attraction
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

    private fun photo(spotId: Long, objectKey: String) = object : com.peakda.server.domain.spot.repository.SpotPhoto {
        override val spotId: Long = spotId
        override val objectKey: String = objectKey
    }

    private fun recordCount(spotId: Long, count: Long) = object : SpotRecordCount {
        override val spotId: Long = spotId
        override val recordCount: Long = count
    }

    companion object {
        private const val SPOT_ID = 100L
        private const val MISSING_SPOT_ID = 999L
        private const val SECOND_SPOT_ID = 200L
        private const val ATTRACTION_ID = 501L
        private const val USER_ID = 7L
    }
}
