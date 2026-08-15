package com.peakda.server.domain.explore.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.common.page.PageRequest
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.curation.application.CurationQueryService
import com.peakda.server.domain.curation.presentation.response.CurationCardResponse
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.application.FestivalDetailProperties
import com.peakda.server.domain.festival.application.FestivalPhase
import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.Estimator
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

class ExploreServiceTest {

    private val seasonalBloomEstimateRepository = mock(SeasonalBloomEstimateRepository::class.java)
    private val attractionRepository = mock(AttractionRepository::class.java)
    private val spotRepository = mock(SpotRepository::class.java)
    private val spotFavoriteRepository = mock(SpotFavoriteRepository::class.java)
    private val festivalRepository = mock(FestivalRepository::class.java)
    private val festivalEditorialRepository = mock(FestivalEditorialRepository::class.java)
    private val objectKeyUrlResolver = mock(ObjectKeyUrlResolver::class.java)
    private val curationQueryService = mock(CurationQueryService::class.java)
    private val properties = ExploreProperties(
        peakNowSize = 3,
        nextWeekSize = 5,
        festivalSize = 3,
        festivalCandidateSize = 20,
        curationSize = 5,
    )
    private val service = ExploreService(
        seasonalBloomEstimateRepository,
        attractionRepository,
        spotRepository,
        spotFavoriteRepository,
        festivalRepository,
        festivalEditorialRepository,
        objectKeyUrlResolver,
        FestivalDetailProperties(endingSoonDays = 7),
        curationQueryService,
        properties,
    )
    private val peakPageable = SpringPageRequest.of(0, 3)
    private val nextWeekPageable = SpringPageRequest.of(0, 5)
    private val festivalPageable = SpringPageRequest.of(0, 20)
    private val curationPageable = SpringPageRequest.of(0, 5)

    @Test
    fun `산출 기준일이 없으면 스팟은 비고 축제와 큐레이션은 채운다`() {
        val festival = festival(701L, "진해 군항제", TODAY.minusDays(1), TODAY.plusDays(4))
        val curation = curationCard()
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(null)
        `when`(festivalRepository.findOngoing(TODAY, festivalPageable)).thenReturn(listOf(festival))
        `when`(curationQueryService.cards(curationPageable)).thenReturn(PageImpl(listOf(curation)))

        val response = service.explore(USER_ID, category = null, today = TODAY)

        assertThat(response.baseDate).isNull()
        assertThat(response.peakNow).isEmpty()
        assertThat(response.nextWeek).isEmpty()
        assertThat(response.festivals.map { it.festivalId }).containsExactly(701L)
        assertThat(response.curations).containsExactly(curation)
        verify(seasonalBloomEstimateRepository, never()).findAttractionIdsByBaseDateAndStatus(
            BASE_DATE,
            BloomStatus.PEAK,
            peakPageable,
        )
    }

    @Test
    fun `절정과 다음 주 섹션은 각각 PEAK와 STARTED 상태로 정해진 크기만큼 조회한다`() {
        stubEmptyExplore()

        service.explore(USER_ID, category = null, today = TODAY)

        verify(seasonalBloomEstimateRepository).findAttractionIdsByBaseDateAndStatus(
            BASE_DATE,
            BloomStatus.PEAK,
            peakPageable,
        )
        verify(seasonalBloomEstimateRepository).findAttractionIdsByBaseDateAndStatus(
            BASE_DATE,
            BloomStatus.STARTED,
            nextWeekPageable,
        )
    }

    @Test
    fun `명소별 최고 신뢰도 추정을 고르고 동률은 카테고리 이름 오름차순으로 고정한다`() {
        stubFestivalAndCuration()
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(BASE_DATE)
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.PEAK,
                peakPageable,
            ),
        ).thenReturn(PageImpl(listOf(1L, 2L), peakPageable, 2))
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.STARTED,
                nextWeekPageable,
            ),
        ).thenReturn(PageImpl(emptyList(), nextWeekPageable, 0))
        `when`(
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(BASE_DATE, listOf(1L, 2L)),
        ).thenReturn(
            listOf(
                estimate(1L, BloomCategory.CHERRY, BloomStatus.PEAK, confidence = 0.8),
                estimate(1L, BloomCategory.PLUM, BloomStatus.PEAK, confidence = 0.9),
                estimate(2L, BloomCategory.PLUM, BloomStatus.PEAK, confidence = 0.7),
                estimate(2L, BloomCategory.AZALEA, BloomStatus.PEAK, confidence = 0.7),
            ),
        )
        `when`(attractionRepository.findAllById(listOf(1L, 2L))).thenReturn(
            listOf(attraction(1L, "첫 명소"), attraction(2L, "둘째 명소")),
        )
        `when`(spotRepository.findByTypeAndAttractionIdIn(SpotType.ATTRACTION, listOf(1L, 2L)))
            .thenReturn(emptyList())

        val response = service.explore(USER_ID, category = null, today = TODAY)

        assertThat(response.peakNow.map { it.category })
            .containsExactly(BloomCategory.PLUM, BloomCategory.AZALEA)
    }

    @Test
    fun `없는 명소와 비노출 명소는 제외해도 원본 페이지 메타를 유지한다`() {
        val request = PageRequest(page = 1, size = 3)
        val pageable = request.toPageable()
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(BASE_DATE)
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.PEAK,
                pageable,
            ),
        ).thenReturn(PageImpl(listOf(1L, 2L, 3L), pageable, 8))
        `when`(
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(BASE_DATE, listOf(1L, 2L, 3L)),
        ).thenReturn(
            listOf(
                estimate(1L, BloomCategory.CHERRY, BloomStatus.PEAK),
                estimate(2L, BloomCategory.PLUM, BloomStatus.PEAK),
                estimate(3L, BloomCategory.CANOLA, BloomStatus.PEAK),
            ),
        )
        `when`(attractionRepository.findAllById(listOf(1L, 2L, 3L))).thenReturn(
            listOf(attraction(1L, "노출"), attraction(3L, "비노출", visible = false)),
        )
        `when`(spotRepository.findByTypeAndAttractionIdIn(SpotType.ATTRACTION, listOf(1L, 2L, 3L)))
            .thenReturn(emptyList())

        val response = service.spots(USER_ID, ExploreSection.PEAK_NOW, category = null, request)

        assertThat(response.content.map { it.attractionId }).containsExactly(1L)
        assertThat(response.page).isEqualTo(1)
        assertThat(response.size).isEqualTo(3)
        assertThat(response.totalElements).isEqualTo(8)
        assertThat(response.totalPages).isEqualTo(3)
        assertThat(response.hasNext).isTrue()
    }

    @Test
    fun `스팟과 찜을 두 섹션 id로 한 번에 조회해 찜과 알림 상태를 채운다`() {
        stubFestivalAndCuration()
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(BASE_DATE)
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.PEAK,
                peakPageable,
            ),
        ).thenReturn(PageImpl(listOf(1L), peakPageable, 1))
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.STARTED,
                nextWeekPageable,
            ),
        ).thenReturn(PageImpl(listOf(2L, 3L), nextWeekPageable, 2))
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(BASE_DATE, listOf(1L)))
            .thenReturn(listOf(estimate(1L, BloomCategory.CHERRY, BloomStatus.PEAK)))
        `when`(seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(BASE_DATE, listOf(2L, 3L)))
            .thenReturn(
                listOf(
                    estimate(2L, BloomCategory.PLUM, BloomStatus.STARTED),
                    estimate(3L, BloomCategory.CANOLA, BloomStatus.STARTED),
                ),
            )
        `when`(attractionRepository.findAllById(listOf(1L, 2L, 3L))).thenReturn(
            listOf(attraction(1L, "절정"), attraction(2L, "피기 시작"), attraction(3L, "비노출 스팟")),
        )
        `when`(spotRepository.findByTypeAndAttractionIdIn(SpotType.ATTRACTION, listOf(1L, 2L, 3L))).thenReturn(
            listOf(
                spot(101L, 1L),
                spot(202L, 2L),
                spot(303L, 3L, visible = false),
            ),
        )
        `when`(spotFavoriteRepository.findByUserIdAndSpotIdIn(USER_ID, listOf(101L, 202L))).thenReturn(
            listOf(SpotFavorite(USER_ID, 101L, notifyEnabled = false)),
        )

        val response = service.explore(USER_ID, category = null, today = TODAY)

        assertThat(response.peakNow.single().spotId).isEqualTo(101L)
        assertThat(response.peakNow.single().favorited).isTrue()
        assertThat(response.peakNow.single().notifyEnabled).isFalse()
        assertThat(response.nextWeek.map { it.spotId }).containsExactly(202L, null)
        assertThat(response.nextWeek.map { it.favorited }).containsExactly(false, false)
        assertThat(response.nextWeek.map { it.notifyEnabled }).containsExactly(false, false)
        verify(attractionRepository, times(1)).findAllById(listOf(1L, 2L, 3L))
        verify(spotRepository, times(1)).findByTypeAndAttractionIdIn(SpotType.ATTRACTION, listOf(1L, 2L, 3L))
        verify(spotFavoriteRepository, times(1)).findByUserIdAndSpotIdIn(USER_ID, listOf(101L, 202L))
    }

    @Test
    fun `진행 중 꽃축제만 남겨 종료일과 지역 정보를 계산한다`() {
        val festivals = listOf(
            festival(
                701L,
                "진해 군항제",
                TODAY.minusDays(2),
                TODAY.plusDays(4),
                roadAddress = "경상남도 창원시 진해구 통신동",
            ),
            festival(
                702L,
                "국제 마라톤 대회",
                TODAY.minusDays(1),
                TODAY.plusDays(2),
                roadAddress = "서울특별시 중구 세종대로",
            ),
            festival(
                703L,
                "광양 매화 축제",
                TODAY,
                null,
                landLotAddress = "전라남도 광양시 다압면",
            ),
        )
        `when`(festivalRepository.findOngoing(TODAY, festivalPageable)).thenReturn(festivals)

        val response = service.festivals(category = null, today = TODAY)

        assertThat(response.items.map { it.festivalId }).containsExactly(701L, 703L)
        assertThat(response.items.map { it.endsInDays }).containsExactly(4L, null)
        assertThat(response.items.map { it.region }).containsExactly("경상남도 창원시", "전라남도 광양시")
    }

    @Test
    fun `축제 목록은 상세와 같은 phase 판정과 발행 hero 이미지를 사용한다`() {
        val festival = festival(
            701L,
            "진해 군항제",
            TODAY.minusDays(2),
            TODAY.plusDays(4),
            roadAddress = "경상남도 창원시 진해구 통신동",
        )
        val editorial = FestivalEditorial(
            festivalId = 701L,
            heroImageUrl = "https://img/hero.jpg",
            status = FestivalEditorialStatus.PUBLISHED,
        )
        `when`(festivalRepository.findOngoing(TODAY, festivalPageable)).thenReturn(listOf(festival))
        `when`(
            festivalEditorialRepository.findByFestivalIdInAndStatus(
                listOf(701L),
                FestivalEditorialStatus.PUBLISHED,
            ),
        ).thenReturn(listOf(editorial))
        doReturn("https://img/hero.jpg").`when`(objectKeyUrlResolver).resolve("https://img/hero.jpg")

        val response = service.festivals(category = null, today = TODAY).items.single()

        assertThat(response.phase).isEqualTo(FestivalPhase.ENDING_SOON)
        assertThat(response.thumbnailUrl).isEqualTo("https://img/hero.jpg")
    }

    @Test
    fun `카테고리 필터는 지정 파인더와 같은 꽃축제 카테고리에 적용한다`() {
        stubFestivalAndCuration(
            festivals = listOf(
                festival(701L, "진해 군항제", TODAY.minusDays(1), TODAY.plusDays(1)),
                festival(702L, "광양 매화 축제", TODAY.minusDays(1), TODAY.plusDays(2)),
            ),
        )
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(BASE_DATE)
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatusAndBloomCategory(
                BASE_DATE,
                BloomStatus.PEAK,
                BloomCategory.CHERRY,
                peakPageable,
            ),
        ).thenReturn(PageImpl(emptyList(), peakPageable, 0))
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatusAndBloomCategory(
                BASE_DATE,
                BloomStatus.STARTED,
                BloomCategory.CHERRY,
                nextWeekPageable,
            ),
        ).thenReturn(PageImpl(emptyList(), nextWeekPageable, 0))

        val response = service.explore(USER_ID, BloomCategory.CHERRY, TODAY)

        assertThat(response.festivals.map { it.festivalId }).containsExactly(701L)
        verify(seasonalBloomEstimateRepository, never()).findAttractionIdsByBaseDateAndStatus(
            BASE_DATE,
            BloomStatus.PEAK,
            peakPageable,
        )
    }

    @Test
    fun `전체 보기 페이지 요청을 정렬 없이 그대로 전달하고 원본 메타를 반환한다`() {
        val request = PageRequest(page = 2, size = 4)
        val pageable = request.toPageable()
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(BASE_DATE)
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.STARTED,
                pageable,
            ),
        ).thenReturn(PageImpl(emptyList(), pageable, 13))

        val response = service.spots(USER_ID, ExploreSection.NEXT_WEEK, category = null, request)

        verify(seasonalBloomEstimateRepository).findAttractionIdsByBaseDateAndStatus(
            BASE_DATE,
            BloomStatus.STARTED,
            pageable,
        )
        assertThat(response.page).isEqualTo(2)
        assertThat(response.size).isEqualTo(4)
        assertThat(response.totalElements).isEqualTo(13)
        assertThat(response.totalPages).isEqualTo(4)
        assertThat(response.hasNext).isTrue()
    }

    private fun stubEmptyExplore() {
        `when`(seasonalBloomEstimateRepository.findLatestBaseDate()).thenReturn(BASE_DATE)
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.PEAK,
                peakPageable,
            ),
        ).thenReturn(PageImpl(emptyList(), peakPageable, 0))
        `when`(
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(
                BASE_DATE,
                BloomStatus.STARTED,
                nextWeekPageable,
            ),
        ).thenReturn(PageImpl(emptyList(), nextWeekPageable, 0))
        stubFestivalAndCuration()
    }

    private fun stubFestivalAndCuration(festivals: List<Festival> = emptyList()) {
        `when`(festivalRepository.findOngoing(TODAY, festivalPageable)).thenReturn(festivals)
        `when`(curationQueryService.cards(curationPageable)).thenReturn(PageImpl(emptyList()))
    }

    private fun estimate(
        attractionId: Long,
        category: BloomCategory,
        status: BloomStatus,
        confidence: Double = 0.9,
    ): SeasonalBloomEstimate = SeasonalBloomEstimate(
        attractionId = attractionId,
        bloomCategory = category,
        baseDate = BASE_DATE,
        status = status,
        confidence = confidence,
        chosenEstimator = Estimator.CALENDAR,
        peakStartDate = TODAY.plusDays(1),
        peakEndDate = TODAY.plusDays(6),
    )

    private fun attraction(id: Long, title: String, visible: Boolean = true): Attraction {
        val attraction = Attraction(
            tourApiContentId = "content-$id",
            title = title,
            addressMain = "서울특별시 종로구",
            longitude = 126.98,
            latitude = 37.55,
            primaryImageUrl = "https://img/$id.jpg",
            visible = visible,
        )
        ReflectionTestUtils.setField(attraction, "id", id)
        return attraction
    }

    private fun spot(id: Long, attractionId: Long, visible: Boolean = true): Spot {
        val spot = Spot(
            type = SpotType.ATTRACTION,
            attractionId = attractionId,
            name = "명소형 스팟 $id",
            latitude = 37.55,
            longitude = 126.98,
            visible = visible,
        )
        ReflectionTestUtils.setField(spot, "id", id)
        return spot
    }

    private fun festival(
        id: Long,
        name: String,
        startsOn: LocalDate,
        endsOn: LocalDate?,
        roadAddress: String? = null,
        landLotAddress: String? = null,
    ): Festival {
        val festival = Festival(
            name = name,
            venue = "축제장",
            startDate = startsOn.toString(),
            endDate = endsOn?.toString(),
            startsOn = startsOn,
            endsOn = endsOn,
            roadAddress = roadAddress,
            landLotAddress = landLotAddress,
            latitude = 35.15,
            longitude = 128.66,
            homepageUrl = "https://festival.example/$id",
        )
        ReflectionTestUtils.setField(festival, "id", id)
        return festival
    }

    private fun curationCard(): CurationCardResponse = CurationCardResponse(
        id = 901L,
        weekLabel = "4월 1주차 · 4/1~4/7",
        weekStartDate = TODAY,
        weekEndDate = TODAY.plusDays(6),
        title = "이번 주말 어디로 갈까요?",
        subtitle = "벚꽃이 가장 예쁜 세 곳",
        heroImageUrl = "https://img/curation.jpg",
    )

    companion object {
        private const val USER_ID = 42L
        private val TODAY = LocalDate.of(2026, 4, 1)
        private val BASE_DATE = LocalDate.of(2026, 3, 31)
    }
}
