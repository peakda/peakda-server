package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotPhoto
import com.peakda.server.domain.spot.repository.SpotRecordCount
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.time.LocalDate

class SpotFavoriteListAssemblerTest {

    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val spotRecordPhotoRepository = mock(SpotRecordPhotoRepository::class.java)
    private val spotRecordPhotoUploader = mock(SpotRecordPhotoUploader::class.java)
    private val attractionRepository = mock(AttractionRepository::class.java)
    private val properties = SpotFavoriteProperties(bannerLeadDays = 7, photoLimit = 4)

    private val assembler = SpotFavoriteListAssembler(
        seasonalBloomEstimateRepository,
        spotRecordRepository,
        spotRecordPhotoRepository,
        spotRecordPhotoUploader,
        attractionRepository,
        properties,
    )

    @Test
    fun `명소형 카드는 상태 우선순위로 대표 개화를 고르고 종료된 추정을 칩에서 제외한다`() {
        val card = card(SPOT_ID, ATTRACTION_ID, SpotType.ATTRACTION, "진해 군항제")
        val estimates = listOf(
            estimate(ATTRACTION_ID, BloomCategory.CANOLA, BloomStatus.STARTED, confidence = 0.99),
            estimate(ATTRACTION_ID, BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.60),
            estimate(ATTRACTION_ID, BloomCategory.MAPLE, BloomStatus.ENDED, confidence = 1.0),
        )

        val response = assemble(listOf(card), baseDate = BASE_DATE, estimates = estimates)
        val favorite = response.favorites.single()

        assertThat(favorite.bloom?.category).isEqualTo(BloomCategory.CHERRY)
        assertThat(favorite.bloom?.status).isEqualTo(BloomStatus.PEAK)
        assertThat(favorite.bloom?.baseDate).isEqualTo(BASE_DATE)
        assertThat(favorite.categories.map { it.category })
            .containsExactly(BloomCategory.CHERRY, BloomCategory.CANOLA)
    }

    @Test
    fun `동네형 카드는 개화 정보가 없고 개화 저장소를 조회하지 않는다`() {
        val card = card(SPOT_ID, null, SpotType.LOCAL, "우리 동네 공원")

        val response = assemble(listOf(card))

        assertThat(response.favorites.single().bloom).isNull()
        assertThat(response.favorites.single().categories).isEmpty()
        verifyNoInteractions(seasonalBloomEstimateRepository)
        verifyNoInteractions(attractionRepository)
    }

    @Test
    fun `배너는 만개 임박 후보보다 지금 절정 진행 중인 후보를 우선한다`() {
        val imminent = card(101L, 501L, SpotType.ATTRACTION, "봄 정원")
        val peak = card(102L, 502L, SpotType.ATTRACTION, "서울숲")
        val estimates = listOf(
            estimate(
                501L,
                BloomCategory.CANOLA,
                BloomStatus.PREPARING,
                peakStartDate = TODAY.plusDays(1),
            ),
            estimate(
                502L,
                BloomCategory.CHERRY,
                BloomStatus.PEAK,
                peakStartDate = TODAY.minusDays(2),
                peakEndDate = TODAY.plusDays(2),
            ),
        )

        val response = assemble(listOf(imminent, peak), baseDate = BASE_DATE, estimates = estimates)

        assertThat(response.banner?.spotId).isEqualTo(102L)
        assertThat(response.banner?.message).isEqualTo("지금 서울숲이 절정이에요")
    }

    @Test
    fun `절정 후보가 없으면 만개까지 가장 적게 남은 후보를 배너로 고른다`() {
        val later = card(101L, 501L, SpotType.ATTRACTION, "봄 정원")
        val sooner = card(102L, 502L, SpotType.ATTRACTION, "여의도")
        val estimates = listOf(
            estimate(501L, BloomCategory.CANOLA, BloomStatus.STARTED, peakStartDate = TODAY.plusDays(5)),
            estimate(502L, BloomCategory.CHERRY, BloomStatus.PREPARING, peakStartDate = TODAY.plusDays(2)),
        )

        val response = assemble(listOf(later, sooner), baseDate = BASE_DATE, estimates = estimates)

        assertThat(response.banner?.spotId).isEqualTo(102L)
        assertThat(response.banner?.daysUntilPeak).isEqualTo(2L)
    }

    @Test
    fun `만개가 오늘 시작하면 추정 상태가 아직 절정이 아니어도 절정 배너로 잡는다`() {
        val startsToday = card(101L, 501L, SpotType.ATTRACTION, "여의도")
        val estimates = listOf(
            estimate(
                501L,
                BloomCategory.CHERRY,
                BloomStatus.PREPARING,
                peakStartDate = TODAY,
                peakEndDate = TODAY.plusDays(5),
            ),
        )

        val response = assemble(listOf(startsToday), baseDate = BASE_DATE, estimates = estimates)

        assertThat(response.banner?.spotId).isEqualTo(101L)
        assertThat(response.banner?.daysUntilPeak).isEqualTo(0L)
        assertThat(response.banner?.message).isEqualTo("지금 여의도가 절정이에요")
    }

    @Test
    fun `임박 창 밖이거나 만개 시작일이 없는 후보뿐이면 배너가 없다`() {
        val outside = card(101L, 501L, SpotType.ATTRACTION, "먼 봄")
        val unknown = card(102L, 502L, SpotType.ATTRACTION, "미정 정원")
        val estimates = listOf(
            estimate(501L, BloomCategory.CANOLA, BloomStatus.PREPARING, peakStartDate = TODAY.plusDays(8)),
            estimate(502L, BloomCategory.CHERRY, BloomStatus.STARTED, peakStartDate = null),
        )

        val response = assemble(listOf(outside, unknown), baseDate = BASE_DATE, estimates = estimates)

        assertThat(response.banner).isNull()
    }

    @Test
    fun `배너 문구는 스팟명 종성에 따라 이와 가를 고른다`() {
        assertThat(SpotFavoriteBannerMessage.imminent("서울")).isEqualTo("서울이 곧 만개해요")
        assertThat(SpotFavoriteBannerMessage.imminent("여의도")).isEqualTo("여의도가 곧 만개해요")
    }

    @Test
    fun `기록 사진은 설정한 최대 장수만 presigned URL로 채운다`() {
        val card = card(SPOT_ID, null, SpotType.LOCAL, "우리 동네 공원")
        val photos = (1..5).map { photo(SPOT_ID, "photo-$it") }
        photos.forEach { projection ->
            `when`(spotRecordPhotoUploader.presignedUrlOf(projection.objectKey))
                .thenReturn("https://cdn/${projection.objectKey}")
        }

        val response = assemble(listOf(card), photos = photos)

        assertThat(response.favorites.single().photoUrls).containsExactly(
            "https://cdn/photo-1",
            "https://cdn/photo-2",
            "https://cdn/photo-3",
            "https://cdn/photo-4",
        )
    }

    @Test
    fun `기록 사진이 없는 명소형은 대표 이미지로 대체하고 동네형은 빈 목록을 유지한다`() {
        val attractionCard = card(101L, 501L, SpotType.ATTRACTION, "봄 정원")
        val localCard = card(102L, null, SpotType.LOCAL, "우리 동네 공원")
        val attraction = attraction(501L, primaryImageUrl = "https://img/primary.jpg")

        val response = assemble(
            cards = listOf(attractionCard, localCard),
            attractions = listOf(attraction),
        )

        assertThat(response.favorites[0].photoUrls).containsExactly("https://img/primary.jpg")
        assertThat(response.favorites[1].photoUrls).isEmpty()
        verify(attractionRepository).findAllById(listOf(501L))
    }

    @Test
    fun `기록 수 집계에 없는 스팟은 0으로 채운다`() {
        val counted = card(101L, null, SpotType.LOCAL, "기록 있는 곳")
        val empty = card(102L, null, SpotType.LOCAL, "기록 없는 곳")

        val response = assemble(
            cards = listOf(counted, empty),
            recordCounts = listOf(recordCount(101L, 5)),
        )

        assertThat(response.favorites.map { it.recordCount }).containsExactly(5L, 0L)
    }

    @Test
    fun `최신 개화 산출일이 없으면 모든 카드의 개화와 배너가 없다`() {
        val first = card(101L, 501L, SpotType.ATTRACTION, "봄 정원")
        val second = card(102L, 502L, SpotType.ATTRACTION, "여의도")

        val response = assemble(cards = listOf(first, second), baseDate = null)

        assertThat(response.favorites).allSatisfy {
            assertThat(it.bloom).isNull()
            assertThat(it.categories).isEmpty()
        }
        assertThat(response.banner).isNull()
        verify(seasonalBloomEstimateRepository).findLatestBaseDate()
        verifyNoMoreInteractions(seasonalBloomEstimateRepository)
    }

    private fun assemble(
        cards: List<Pair<SpotFavorite, Spot>>,
        baseDate: LocalDate? = null,
        estimates: List<SeasonalBloomEstimate> = emptyList(),
        recordCounts: List<SpotRecordCount> = emptyList(),
        photos: List<SpotPhoto> = emptyList(),
        attractions: List<Attraction> = emptyList(),
    ) = cards.let { favoriteCards ->
        val spotIds = favoriteCards.map { requireNotNull(it.second.id) }
        `when`(spotRecordRepository.countBySpotIdInAndStatus(spotIds, SpotRecordStatus.PUBLISHED))
            .thenReturn(recordCounts)
        `when`(
            spotRecordPhotoRepository.findRecentPhotosBySpotIds(
                spotIds,
                SpotRecordStatus.PUBLISHED.name,
                properties.photoLimit,
            ),
        ).thenReturn(photos)

        val attractionIds = favoriteCards
            .mapNotNull { (_, spot) -> spot.attractionId.takeIf { spot.type == SpotType.ATTRACTION } }
            .distinct()
        if (attractionIds.isNotEmpty()) {
            `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(baseDate)
            if (baseDate != null) {
                `when`(
                    seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, attractionIds),
                ).thenReturn(estimates)
            }
        }

        val photoSpotIds = photos.map { it.spotId }.toSet()
        val fallbackAttractionIds = favoriteCards
            .filter { (_, spot) -> requireNotNull(spot.id) !in photoSpotIds }
            .mapNotNull { (_, spot) -> spot.attractionId.takeIf { spot.type == SpotType.ATTRACTION } }
            .distinct()
        if (fallbackAttractionIds.isNotEmpty()) {
            `when`(attractionRepository.findAllById(fallbackAttractionIds)).thenReturn(attractions)
        }

        assembler.assemble(
            favorites = favoriteCards.map { it.first },
            spotsById = favoriteCards.associate { requireNotNull(it.second.id) to it.second },
            today = TODAY,
        )
    }

    private fun card(
        spotId: Long,
        attractionId: Long?,
        type: SpotType,
        name: String,
    ): Pair<SpotFavorite, Spot> {
        val favorite = SpotFavorite(userId = USER_ID, spotId = spotId)
        ReflectionTestUtils.setField(favorite, "id", spotId + 1_000)
        ReflectionTestUtils.setField(favorite, "createdAt", Instant.parse("2026-03-20T00:00:00Z"))

        val spot = Spot(
            type = type,
            attractionId = attractionId,
            name = name,
            address = "주소",
            latitude = 37.0,
            longitude = 127.0,
        )
        ReflectionTestUtils.setField(spot, "id", spotId)
        return favorite to spot
    }

    private fun estimate(
        attractionId: Long,
        category: BloomCategory,
        status: BloomStatus,
        confidence: Double = 0.8,
        peakStartDate: LocalDate? = TODAY.plusDays(3),
        peakEndDate: LocalDate? = TODAY.plusDays(7),
    ) = SeasonalBloomEstimate(
        attractionId = attractionId,
        bloomCategory = category,
        baseDate = BASE_DATE,
        status = status,
        confidence = confidence,
        chosenEstimator = Estimator.CALENDAR,
        peakStartDate = peakStartDate,
        peakEndDate = peakEndDate,
        peakDurationDays = 5,
    )

    private fun attraction(
        id: Long,
        primaryImageUrl: String?,
        thumbnailImageUrl: String? = null,
    ) = Attraction(
        tourApiContentId = "content-$id",
        title = "명소 $id",
        primaryImageUrl = primaryImageUrl,
        thumbnailImageUrl = thumbnailImageUrl,
    ).also { ReflectionTestUtils.setField(it, "id", id) }

    private fun recordCount(spotId: Long, count: Long) = object : SpotRecordCount {
        override val spotId: Long = spotId
        override val recordCount: Long = count
    }

    private fun photo(spotId: Long, objectKey: String) = object : SpotPhoto {
        override val spotId: Long = spotId
        override val objectKey: String = objectKey
    }

    companion object {
        private const val USER_ID = 7L
        private const val SPOT_ID = 100L
        private const val ATTRACTION_ID = 500L
        private val TODAY = LocalDate.of(2026, 3, 25)
        private val BASE_DATE = LocalDate.of(2026, 3, 24)
    }
}
