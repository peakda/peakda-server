package com.peakda.server.domain.sitemap.application

import com.peakda.server.common.persistence.LastModifiedRow
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.repository.CurationRepository
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.sitemap.presentation.response.SitemapResponse
import com.peakda.server.domain.sitemap.presentation.response.SitemapResponse.SitemapEntry
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * 웹 sitemap 을 만들 공개 상세 페이지 목록. 비로그인으로 열리는 상세만 담는다.
 *
 * - 스팟: 공개 명소형 스팟. 동네(LOCAL) 스팟은 사용자 기록으로 생긴 지점이라 검색 노출 대상이 아니다.
 * - 축제: 상세가 모든 축제에 열려 있으므로 전체.
 * - 큐레이션: 발행된 것만.
 *
 * 수정 시각은 상세 화면 내용의 마지막 변경이다. 스팟은 게시 기록, 축제는 발행 에디토리얼의 변경까지 반영한다.
 * 매일 다시 산출되는 개화 추정은 반영하지 않는다.
 */
@Service
class SitemapService(
    private val spotRepository: SpotRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val festivalRepository: FestivalRepository,
    private val festivalEditorialRepository: FestivalEditorialRepository,
    private val curationRepository: CurationRepository,
) {

    @Transactional(readOnly = true)
    fun sitemap(): SitemapResponse = SitemapResponse(
        spots = spots(),
        festivals = festivals(),
        curations = curationRepository.findByStatusOrderByIdAsc(CurationStatus.PUBLISHED).map { it.toEntry() },
    )

    private fun spots(): List<SitemapEntry> {
        val recordModifiedAtBySpotId = spotRecordRepository
            .findLastModifiedAtPerSpotByStatus(SpotRecordStatus.PUBLISHED)
            .associate { it.spotId to it.lastModifiedAt }
        return spotRepository.findByTypeAndVisibleTrueOrderByIdAsc(SpotType.ATTRACTION)
            .map { it.toEntry(recordModifiedAtBySpotId[it.id]) }
    }

    private fun festivals(): List<SitemapEntry> {
        val editorialModifiedAtByFestivalId = festivalEditorialRepository
            .findByStatus(FestivalEditorialStatus.PUBLISHED)
            .associate { it.festivalId to it.updatedAt }
        return festivalRepository.findAllByOrderByIdAsc()
            .map { it.toEntry(editorialModifiedAtByFestivalId[it.id]) }
    }

    private fun LastModifiedRow.toEntry(childModifiedAt: Instant? = null): SitemapEntry = SitemapEntry(
        id = id,
        updatedAt = if (childModifiedAt != null && childModifiedAt > updatedAt) childModifiedAt else updatedAt,
    )
}
