package com.peakda.server.domain.sitemap.application

import com.peakda.server.common.persistence.LastModifiedRow
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.repository.CurationRepository
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.repository.FestivalEditorialLastModifiedAt
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.sitemap.presentation.response.SitemapResponse.SitemapEntry
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRecordLastModifiedAt
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant

class SitemapServiceTest {

    private val spotRepository = mock(SpotRepository::class.java)
    private val spotRecordRepository = mock(SpotRecordRepository::class.java)
    private val festivalRepository = mock(FestivalRepository::class.java)
    private val festivalEditorialRepository = mock(FestivalEditorialRepository::class.java)
    private val curationRepository = mock(CurationRepository::class.java)
    private val service = SitemapService(
        spotRepository,
        spotRecordRepository,
        festivalRepository,
        festivalEditorialRepository,
        curationRepository,
    )

    @Test
    fun `공개 명소 스팟은 게시 기록이 더 최근이면 기록 수정 시각을 쓴다`() {
        `when`(spotRepository.findByTypeAndVisibleTrueOrderByIdAsc(SpotType.ATTRACTION))
            .thenReturn(listOf(row(1L, MAY_1), row(2L, MAY_3), row(3L, MAY_1)))
        `when`(spotRecordRepository.findLastModifiedAtPerSpotByStatus(SpotRecordStatus.PUBLISHED))
            .thenReturn(listOf(recordAt(1L, MAY_3), recordAt(2L, MAY_1), recordAt(99L, MAY_3)))

        val response = service.sitemap()

        assertThat(response.spots).containsExactly(
            SitemapEntry(1L, MAY_3),
            SitemapEntry(2L, MAY_3),
            SitemapEntry(3L, MAY_1),
        )
    }

    @Test
    fun `축제는 전체를 담고 발행 에디토리얼이 더 최근이면 에디토리얼 수정 시각을 쓴다`() {
        `when`(festivalRepository.findAllByOrderByIdAsc()).thenReturn(listOf(row(10L, MAY_1), row(11L, MAY_1)))
        `when`(festivalEditorialRepository.findByStatus(FestivalEditorialStatus.PUBLISHED))
            .thenReturn(listOf(editorialAt(11L, MAY_3)))

        val response = service.sitemap()

        assertThat(response.festivals).containsExactly(SitemapEntry(10L, MAY_1), SitemapEntry(11L, MAY_3))
    }

    @Test
    fun `큐레이션은 발행된 것만 담는다`() {
        `when`(curationRepository.findByStatusOrderByIdAsc(CurationStatus.PUBLISHED)).thenReturn(listOf(row(5L, MAY_3)))

        val response = service.sitemap()

        assertThat(response.curations).containsExactly(SitemapEntry(5L, MAY_3))
    }

    private fun row(id: Long, updatedAt: Instant): LastModifiedRow = object : LastModifiedRow {
        override val id = id
        override val updatedAt = updatedAt
    }

    private fun recordAt(spotId: Long, at: Instant): SpotRecordLastModifiedAt = object : SpotRecordLastModifiedAt {
        override val spotId = spotId
        override val lastModifiedAt = at
    }

    private fun editorialAt(festivalId: Long, at: Instant): FestivalEditorialLastModifiedAt =
        object : FestivalEditorialLastModifiedAt {
            override val festivalId = festivalId
            override val updatedAt = at
        }

    companion object {
        private val MAY_1 = Instant.parse("2026-05-01T00:00:00Z")
        private val MAY_3 = Instant.parse("2026-05-03T00:00:00Z")
    }
}
