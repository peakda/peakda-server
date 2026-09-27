package com.peakda.server.domain.seasonal.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.repository.AttractionBloomRepository
import com.peakda.server.domain.seasonal.repository.AttractionBloomUpsertCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.springframework.test.util.ReflectionTestUtils

class BloomTaggingServiceTest {

    private val attractionRepository = mock(AttractionRepository::class.java)
    private val festivalRepository = mock(FestivalRepository::class.java)
    private val attractionBloomRepository = mock(AttractionBloomRepository::class.java)
    private val service = BloomTaggingService(
        attractionRepository,
        festivalRepository,
        attractionBloomRepository,
        BloomTaggingProperties(),
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
    fun `제외어 한국화가 있으면 국화로 태깅하지 않는다`() {
        val count = service.tagKeywords(listOf(attraction(1L, "한국화 미술관")))

        assertThat(count).isZero()
        assertThat(upserts()).isEmpty()
    }

    private fun upserts(): List<AttractionBloomUpsertCommand> =
        mockingDetails(attractionBloomRepository).invocations
            .filter { it.method.name == "upsert" }
            .map { it.arguments[0] as AttractionBloomUpsertCommand }

    private fun attraction(id: Long, title: String): Attraction =
        Attraction(tourApiContentId = "content-$id", contentTypeCode = "12", title = title).also {
            ReflectionTestUtils.setField(it, "id", id)
        }
}
