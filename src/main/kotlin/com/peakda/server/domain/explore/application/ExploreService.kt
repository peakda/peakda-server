package com.peakda.server.domain.explore.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.curation.application.CurationQueryService
import com.peakda.server.domain.explore.presentation.response.ExploreFestivalListResponse
import com.peakda.server.domain.explore.presentation.response.ExploreResponse
import com.peakda.server.domain.explore.presentation.response.ExploreResponse.ExploreFestivalItem
import com.peakda.server.domain.explore.presentation.response.ExploreResponse.ExploreSpotItem
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 탐색 탭(SCR-022) 큐레이션.
 *
 * 개화 추정·명소·스팟·찜·축제·에디터 큐레이션은 도메인별로 조회한 뒤 id로 결합한다.
 * 스팟 섹션 판정과 대표 카테고리 선별, 꽃축제·지역 표시는 application 계층에서 수행한다.
 */
@Service
class ExploreService(
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val attractionRepository: AttractionRepository,
    private val spotRepository: SpotRepository,
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val festivalRepository: FestivalRepository,
    private val curationQueryService: CurationQueryService,
    private val properties: ExploreProperties,
) {

    @Transactional(readOnly = true)
    fun explore(userId: Long, category: BloomCategory?, today: LocalDate): ExploreResponse {
        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate()
        val peakNow = findSection(
            baseDate = baseDate,
            status = BloomStatus.PEAK,
            pageable = SpringPageRequest.of(0, properties.peakNowSize),
            category = category,
        )
        val nextWeek = findSection(
            baseDate = baseDate,
            status = BloomStatus.STARTED,
            pageable = SpringPageRequest.of(0, properties.nextWeekSize),
            category = category,
        )
        val spotAssembly = loadSpotAssembly(listOf(peakNow, nextWeek), userId)

        return ExploreResponse(
            baseDate = baseDate,
            today = today,
            peakNow = peakNow.toItems(spotAssembly),
            nextWeek = nextWeek.toItems(spotAssembly),
            festivals = festivalItems(category, today).take(properties.festivalSize),
            curations = curationQueryService.cards(SpringPageRequest.of(0, properties.curationSize)).content,
        )
    }

    @Transactional(readOnly = true)
    fun spots(
        userId: Long,
        section: ExploreSection,
        category: BloomCategory?,
        pageRequest: PageRequest,
    ): PageResponse<ExploreSpotItem> {
        val sectionData = findSection(
            baseDate = seasonalBloomEstimateRepository.findLatestBaseDate(),
            status = section.status,
            pageable = pageRequest.toPageable(),
            category = category,
        )
        val content = sectionData.toItems(loadSpotAssembly(listOf(sectionData), userId))
        return sectionData.page.toPageResponse(content)
    }

    @Transactional(readOnly = true)
    fun festivals(category: BloomCategory?, today: LocalDate): ExploreFestivalListResponse =
        ExploreFestivalListResponse(festivalItems(category, today))

    private fun findSection(
        baseDate: LocalDate?,
        status: BloomStatus,
        pageable: Pageable,
        category: BloomCategory?,
    ): ExploreSpotSectionData {
        if (baseDate == null) return ExploreSpotSectionData(Page.empty(pageable), emptyMap())
        val page = if (category == null) {
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatus(baseDate, status, pageable)
        } else {
            seasonalBloomEstimateRepository.findAttractionIdsByBaseDateAndStatusAndBloomCategory(
                baseDate,
                status,
                category,
                pageable,
            )
        }
        if (page.isEmpty) return ExploreSpotSectionData(page, emptyMap())

        val estimates = if (category == null) {
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdIn(baseDate, page.content)
        } else {
            seasonalBloomEstimateRepository.findByBaseDateAndAttractionIdInAndBloomCategory(
                baseDate,
                page.content,
                category,
            )
        }
        val representativeByAttraction = estimates
            .filter { it.status == status }
            .groupBy { it.attractionId }
            .mapValues { (_, rows) ->
                rows.sortedWith(
                    compareByDescending<SeasonalBloomEstimate> { it.confidence }
                        .thenBy { it.bloomCategory.name },
                ).first()
            }
        return ExploreSpotSectionData(page, representativeByAttraction)
    }

    private fun loadSpotAssembly(sections: List<ExploreSpotSectionData>, userId: Long): ExploreSpotAssembly {
        val attractionIds = sections.flatMap { it.page.content }.distinct()
        if (attractionIds.isEmpty()) return ExploreSpotAssembly(emptyMap(), emptyMap(), emptyMap())
        val attractionsById = attractionRepository.findAllById(attractionIds)
            .filter { it.visible }
            .associateBy { requireNotNull(it.id) }
        val spotIdByAttraction = spotRepository.findByTypeAndAttractionIdIn(SpotType.ATTRACTION, attractionIds)
            .asSequence()
            .filter { it.visible }
            .mapNotNull { spot ->
                val attractionId = spot.attractionId ?: return@mapNotNull null
                val spotId = spot.id ?: return@mapNotNull null
                attractionId to spotId
            }
            .sortedBy { it.second }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, spotIds) -> spotIds.first() }
        val favoritesBySpot = if (spotIdByAttraction.isEmpty()) {
            emptyMap()
        } else {
            spotFavoriteRepository.findByUserIdAndSpotIdIn(userId, spotIdByAttraction.values.toList())
                .associateBy { it.spotId }
        }
        return ExploreSpotAssembly(attractionsById, spotIdByAttraction, favoritesBySpot)
    }

    private fun ExploreSpotSectionData.toItems(assembly: ExploreSpotAssembly): List<ExploreSpotItem> =
        page.content.mapNotNull { attractionId ->
            val attraction = assembly.attractionsById[attractionId] ?: return@mapNotNull null
            val estimate = representativeByAttraction[attractionId] ?: return@mapNotNull null
            val spotId = assembly.spotIdByAttraction[attractionId]
            estimate.toResponse(attraction, spotId, spotId?.let(assembly.favoritesBySpot::get))
        }

    private fun SeasonalBloomEstimate.toResponse(
        attraction: Attraction,
        spotId: Long?,
        favorite: SpotFavorite?,
    ): ExploreSpotItem = ExploreSpotItem(
        spotId = spotId,
        attractionId = attractionId,
        name = attraction.title,
        address = attraction.addressMain,
        thumbnailUrl = attraction.primaryImageUrl ?: attraction.thumbnailImageUrl,
        category = bloomCategory,
        displayName = bloomCategory.displayName,
        status = status,
        confidence = confidence,
        peakStartDate = peakStartDate,
        peakEndDate = peakEndDate,
        favorited = favorite != null,
        notifyEnabled = favorite?.notifyEnabled ?: false,
        latitude = attraction.latitude,
        longitude = attraction.longitude,
    )

    private fun festivalItems(category: BloomCategory?, today: LocalDate): List<ExploreFestivalItem> {
        val pageable = SpringPageRequest.of(0, properties.festivalCandidateSize)
        return festivalRepository.findOngoing(today, pageable).mapNotNull { festival ->
            val matchedCategory = BloomCategory.ofFestivalName(festival.name) ?: return@mapNotNull null
            if (category != null && matchedCategory != category) return@mapNotNull null
            val startsOn = festival.startsOn ?: return@mapNotNull null
            ExploreFestivalItem(
                festivalId = requireNotNull(festival.id),
                name = festival.name,
                venue = festival.venue,
                region = festival.region(),
                startsOn = startsOn,
                endsOn = festival.endsOn,
                endsInDays = festival.endsOn?.let { ChronoUnit.DAYS.between(today, it) },
                category = matchedCategory,
                displayName = matchedCategory.displayName,
                latitude = festival.latitude,
                longitude = festival.longitude,
                homepageUrl = festival.homepageUrl,
            )
        }
    }

    /** Figma 카드의 지역 표시는 도로명 주소를 우선해 공백 기준 앞 두 토큰만 사용한다. */
    private fun Festival.region(): String? {
        val address = roadAddress ?: landLotAddress ?: return null
        return address.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(" ")
            .ifBlank { null }
    }

    private fun Page<Long>.toPageResponse(content: List<ExploreSpotItem>): PageResponse<ExploreSpotItem> =
        PageResponse(
            content = content,
            page = number,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = hasNext(),
        )

}
