package com.peakda.server.domain.congestion.application

import com.peakda.server.domain.attraction.application.AttractionEligibilityProperties
import com.peakda.server.domain.attraction.repository.AttractionNameCandidate
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.congestion.entity.CongestionAttractionLink
import com.peakda.server.domain.congestion.entity.CongestionLinkStatus
import com.peakda.server.domain.congestion.entity.CongestionMatchType
import com.peakda.server.domain.congestion.repository.CongestionAttractionLinkRepository
import com.peakda.server.domain.congestion.repository.CongestionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CongestionAttractionLinkServiceTest {
    private val congestionRepository = mock(CongestionRepository::class.java)
    private val linkRepository = mock(CongestionAttractionLinkRepository::class.java)
    private val attractionRepository = mock(AttractionRepository::class.java)
    private val service = CongestionAttractionLinkService(
        congestionRepository,
        linkRepository,
        attractionRepository,
        AttractionEligibilityProperties(setOf("12")),
    )

    @Test
    fun `확정·거절된 연결은 다시 평가하지 않고 나머지만 판정해 저장한다`() {
        val confirmed = link("석촌호수", CongestionLinkStatus.CONFIRMED, attractionId = 1)
        val rejected = link("롯데월드", CongestionLinkStatus.REJECTED)
        val unmatched = link("올림픽공원", CongestionLinkStatus.UNMATCHED)
        `when`(linkRepository.findByAreaCodeAndSigunguCode("11", "11710")).thenReturn(listOf(confirmed, rejected, unmatched))
        `when`(attractionRepository.findNameCandidatesBySigungu("11710", setOf("12")))
            .thenReturn(listOf(AttractionNameCandidate(5, "올림픽공원"), AttractionNameCandidate(6, "몽촌토성")))

        val summary = service.linkSigungu("11", "11710", listOf("석촌호수", "롯데월드", "올림픽공원", "몽촌토성"))

        assertThat(summary.evaluated).isEqualTo(2)
        assertThat(summary.skipped).isEqualTo(2)
        assertThat(summary.byStatus).containsEntry(CongestionLinkStatus.CONFIRMED, 2)
        assertThat(unmatched.status).isEqualTo(CongestionLinkStatus.CONFIRMED)
        assertThat(unmatched.attractionId).isEqualTo(5)

        val saved = savedLinks()
        assertThat(saved).extracting<String> { it.touristAttractionName }.containsExactly("올림픽공원", "몽촌토성")
        assertThat(saved.last().attractionId).isEqualTo(6)
        assertThat(saved.last().matchType).isEqualTo(CongestionMatchType.EXACT)
        verify(attractionRepository, never()).findNameCandidatesByAreas(anyCollection(), anyCollection())
    }

    @Test
    fun `시군구 코드에 해당하는 명소가 없으면 개편 후 시도 범위에서 찾는다`() {
        `when`(linkRepository.findByAreaCodeAndSigunguCode("29", "29110")).thenReturn(emptyList())
        `when`(attractionRepository.findNameCandidatesBySigungu("29110", setOf("12"))).thenReturn(emptyList())
        `when`(attractionRepository.findNameCandidatesByAreas(setOf("12"), setOf("12")))
            .thenReturn(listOf(AttractionNameCandidate(8, "양림동 역사문화마을")))

        service.linkSigungu("29", "29110", listOf("양림동 역사문화마을"))

        val saved = savedLinks().single()
        assertThat(saved.status).isEqualTo(CongestionLinkStatus.CONFIRMED)
        assertThat(saved.matchType).isEqualTo(CongestionMatchType.REGION_REFORM)
        assertThat(saved.attractionId).isEqualTo(8)
    }

    @Test
    fun `같은 실행에서 같은 시도의 사라진 시군구가 여럿이면 시도 후보를 한 번만 읽는다`() {
        `when`(linkRepository.findByAreaCodeAndSigunguCode(anyString(), anyString())).thenReturn(emptyList())
        `when`(attractionRepository.findNameCandidatesBySigungu(anyString(), anyCollection())).thenReturn(emptyList())
        `when`(attractionRepository.findNameCandidatesByAreas(setOf("12"), setOf("12")))
            .thenReturn(listOf(AttractionNameCandidate(8, "양림동 역사문화마을")))
        val cache = mutableMapOf<Set<String>, List<AttractionNameCandidate>>()

        service.linkSigungu("29", "29110", listOf("양림동 역사문화마을"), cache)
        service.linkSigungu("46", "46110", listOf("갓바위"), cache)

        verify(attractionRepository, times(1)).findNameCandidatesByAreas(setOf("12"), setOf("12"))
    }

    @Test
    fun `다시 평가할 이름이 없으면 명소를 조회하지 않는다`() {
        `when`(linkRepository.findByAreaCodeAndSigunguCode("11", "11710"))
            .thenReturn(listOf(link("석촌호수", CongestionLinkStatus.CONFIRMED, attractionId = 1)))

        val summary = service.linkSigungu("11", "11710", listOf("석촌호수"))

        assertThat(summary.skipped).isEqualTo(1)
        verify(attractionRepository, never()).findNameCandidatesBySigungu(anyString(), anyCollection())
    }

    @Suppress("UNCHECKED_CAST")
    private fun savedLinks(): List<CongestionAttractionLink> {
        val captor = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<CongestionAttractionLink>>
        verify(linkRepository).saveAll(captor.capture())
        return captor.value.toList()
    }

    private fun link(name: String, status: CongestionLinkStatus, attractionId: Long? = null) =
        CongestionAttractionLink(
            areaCode = "11",
            sigunguCode = "11710",
            touristAttractionName = name,
            attractionId = attractionId,
            status = status,
        )
}
