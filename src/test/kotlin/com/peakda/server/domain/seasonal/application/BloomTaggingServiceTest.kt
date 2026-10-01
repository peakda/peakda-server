package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.TagSource
import com.peakda.server.domain.seasonal.repository.AttractionBloomRepository
import com.peakda.server.domain.seasonal.repository.AttractionBloomUpsertCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.time.LocalDate

class BloomTaggingServiceTest {

    private var nearbyAttractions: List<Attraction> = emptyList()
    private var festivals: List<Festival> = emptyList()
    private val attractionRepository = mock(AttractionRepository::class.java) { invocation ->
        if (invocation.method.name == "findVisibleInBoundingBoxByContentTypes") nearbyAttractions else null
    }
    private val festivalRepository = mock(FestivalRepository::class.java) { invocation ->
        if (invocation.method.name == "findByLatitudeIsNotNullAndLongitudeIsNotNull") festivals else null
    }
    private val attractionBloomRepository = mock(AttractionBloomRepository::class.java)
    private val service = BloomTaggingService(
        attractionRepository,
        festivalRepository,
        attractionBloomRepository,
        BloomTaggingProperties(),
        AttractionEligibilityProperties(setOf("12")),
    )

    @Test
    fun `한글 꽃 이름이 제목에 있으면 키워드 태그를 만든다`() {
        val count = service.tagKeywords(listOf(attraction(1L, "구리 한강공원 코스모스 단지")))

        assertThat(count).isEqualTo(1)
        assertThat(upserts().map { it.bloomCategory }).containsExactly("COSMOS")
    }

    @Test
    fun `영문 COSMOS 제목은 코스모스로 태깅하지 않는다`() {
        val count = service.tagKeywords(listOf(attraction(1L, "COSMOS BIGBANG 20TH ANNIVERSARY MEDIA EXHIBITION")))

        assertThat(count).isZero()
        assertThat(upserts()).isEmpty()
    }

    @Test
    fun `제외어 한국화·수국사가 있으면 해당 꽃으로 태깅하지 않는다`() {
        val count = service.tagKeywords(listOf(attraction(1L, "한국화 미술관"), attraction(2L, "수국사")))

        assertThat(count).isZero()
        assertThat(upserts()).isEmpty()
    }

    @Test
    fun `꽃 이름이 우연히 들어간 화석지·다리 이름은 태깅하지 않는다`() {
        val count = service.tagKeywords(
            listOf(attraction(1L, "여수 낭도리 공룡발자국화석 산지"), attraction(2L, "동백대교")),
        )

        assertThat(count).isZero()
        assertThat(upserts()).isEmpty()
    }

    @Test
    fun `축제 좌표 근처라도 장소명이 다르면 태깅하지 않고 멀리 있어도 장소명이 같으면 태깅한다`() {
        // 축제 좌표는 동구청(도심), 실제 행사장은 약 13km 떨어진 팔공산 갓바위.
        festivals = listOf(
            festival(
                id = 6001L,
                name = "2026년 제25회 팔공산 단풍축제",
                venue = "팔공산 갓바위 시설지구 일원",
                roadAddress = "대구광역시 동구 아양로 207",
                latitude = 35.886807,
                longitude = 128.636074,
            ),
        )
        nearbyAttractions = listOf(
            attraction(1L, "대구예술발전소", latitude = 35.8790, longitude = 128.6280),
            attraction(2L, "팔공산 갓바위", latitude = 35.9770, longitude = 128.7230),
        )

        val count = service.tagFestivals(LocalDate.of(2026, 10, 20))

        assertThat(count).isEqualTo(1)
        val upsert = upserts().single()
        assertThat(upsert.attractionId).isEqualTo(2L)
        assertThat(upsert.bloomCategory).isEqualTo("MAPLE")
        assertThat(upsert.evidence).isEqualTo("festival:6001,name:2026년 제25회 팔공산 단풍축제,token:팔공산")
    }

    @Test
    fun `축제 후보 명소는 서비스 대상 유형으로만 조회한다`() {
        festivals = listOf(
            festival(
                id = 1L,
                name = "강진만 춤추는 갈대축제",
                venue = "강진만 생태공원 일원",
                roadAddress = "전남광주통합특별시 강진군 강진읍 남당로 97-23",
                latitude = 34.6259705,
                longitude = 126.7724317,
            ),
        )

        service.tagFestivals(LocalDate.of(2026, 10, 20))

        val lookup = mockingDetails(attractionRepository).invocations.single()
        assertThat(lookup.method.name).isEqualTo("findVisibleInBoundingBoxByContentTypes")
        assertThat(lookup.arguments[0]).isEqualTo(setOf("12"))
    }

    @Test
    fun `종료 후 유지 기간이 지난 축제는 태깅하지 않는다`() {
        festivals = listOf(
            festival(
                id = 1L,
                name = "팔공산 단풍축제",
                venue = "팔공산",
                roadAddress = null,
                latitude = 35.97,
                longitude = 128.72,
                endsOn = LocalDate.of(2025, 10, 19),
            ),
        )
        nearbyAttractions = listOf(attraction(2L, "팔공산 갓바위", latitude = 35.9770, longitude = 128.7230))

        val count = service.tagFestivals(LocalDate.of(2026, 10, 20))

        assertThat(count).isZero()
    }

    @Test
    fun `종료 후 유지 기간 안의 축제는 계속 태깅한다`() {
        festivals = listOf(
            festival(
                id = 1L,
                name = "팔공산 단풍축제",
                venue = "팔공산",
                roadAddress = null,
                latitude = 35.97,
                longitude = 128.72,
                endsOn = LocalDate.of(2025, 10, 20),
            ),
        )
        nearbyAttractions = listOf(attraction(2L, "팔공산 갓바위", latitude = 35.9770, longitude = 128.7230))

        val count = service.tagFestivals(LocalDate.of(2026, 10, 20))

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `설정한 TourAPI 소분류의 명소에만 분류 태그를 만든다`() {
        val categoryService = BloomTaggingService(
            attractionRepository,
            festivalRepository,
            attractionBloomRepository,
            BloomTaggingProperties(categoryTags = mapOf(BloomCategory.MAPLE to setOf("A01010100"))),
            AttractionEligibilityProperties(setOf("12")),
        )

        val count = categoryService.tagCategories(
            listOf(
                attraction(1L, "주왕산국립공원", categoryMinor = "A01010100"),
                attraction(2L, "해수욕장", categoryMinor = "A01011200"),
                attraction(3L, "분류 없는 명소"),
            ),
        )

        assertThat(count).isEqualTo(1)
        val upsert = upserts().single()
        assertThat(upsert.attractionId).isEqualTo(1L)
        assertThat(upsert.source).isEqualTo("CATEGORY")
        assertThat(upsert.evidence).isEqualTo("category:A01010100")
    }

    @Test
    fun `분류 설정이 비어 있으면 분류 태그를 만들지 않는다`() {
        val count = service.tagCategories(listOf(attraction(1L, "주왕산국립공원", categoryMinor = "A01010100")))

        assertThat(count).isZero()
        assertThat(upserts()).isEmpty()
    }

    @Test
    fun `분류 태그도 자동 출처로 정리할 수 있다`() {
        service.deleteStaleAutoTags(Instant.parse("2026-10-01T20:45:00Z"), setOf(TagSource.CATEGORY))

        val delete = mockingDetails(attractionBloomRepository).invocations.single()
        assertThat(delete.arguments[0] as Collection<*>).containsExactly(TagSource.CATEGORY)
    }

    @Test
    fun `오래된 태그 정리는 요청한 자동 출처만 실행 시작 1시간 전 기준으로 지운다`() {
        val runStartedAt = Instant.parse("2026-09-27T20:45:00Z")

        service.deleteStaleAutoTags(runStartedAt, setOf(TagSource.KEYWORD, TagSource.FESTIVAL))

        val delete = mockingDetails(attractionBloomRepository).invocations.single()
        assertThat(delete.method.name).isEqualTo("deleteBySourceInAndUpdatedAtBefore")
        assertThat(delete.arguments[0] as Collection<*>)
            .containsExactlyInAnyOrder(TagSource.KEYWORD, TagSource.FESTIVAL)
        assertThat(delete.arguments[1]).isEqualTo(Instant.parse("2026-09-27T19:45:00Z"))
    }

    @Test
    fun `정리할 출처가 없으면 삭제 쿼리를 실행하지 않고 수동 태그는 거부한다`() {
        val runStartedAt = Instant.parse("2026-09-27T20:45:00Z")

        assertThat(service.deleteStaleAutoTags(runStartedAt, emptySet())).isZero()
        assertThat(mockingDetails(attractionBloomRepository).invocations).isEmpty()
        assertThatThrownBy { service.deleteStaleAutoTags(runStartedAt, setOf(TagSource.MANUAL)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun upserts(): List<AttractionBloomUpsertCommand> =
        mockingDetails(attractionBloomRepository).invocations
            .filter { it.method.name == "upsert" }
            .map { it.arguments[0] as AttractionBloomUpsertCommand }

    private fun attraction(
        id: Long,
        title: String,
        latitude: Double? = null,
        longitude: Double? = null,
        addressMain: String? = null,
        categoryMinor: String? = null,
    ): Attraction =
        Attraction(
            tourApiContentId = "content-$id",
            contentTypeCode = "12",
            title = title,
            addressMain = addressMain,
            latitude = latitude,
            longitude = longitude,
            categoryMinor = categoryMinor,
        ).also { ReflectionTestUtils.setField(it, "id", id) }

    private fun festival(
        id: Long,
        name: String,
        venue: String,
        roadAddress: String?,
        latitude: Double,
        longitude: Double,
        endsOn: LocalDate = LocalDate.of(2026, 11, 1),
    ): Festival =
        Festival(
            name = name,
            venue = venue,
            startDate = "2026-10-15",
            startsOn = LocalDate.of(2026, 10, 15),
            endsOn = endsOn,
            roadAddress = roadAddress,
            latitude = latitude,
            longitude = longitude,
        ).also { ReflectionTestUtils.setField(it, "id", id) }
}
