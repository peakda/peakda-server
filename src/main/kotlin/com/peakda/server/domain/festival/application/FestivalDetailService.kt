package com.peakda.server.domain.festival.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalHighlight
import com.peakda.server.domain.festival.exception.FestivalNotFoundException
import com.peakda.server.domain.festival.presentation.response.FestivalDetailResponse
import com.peakda.server.domain.festival.presentation.response.FestivalDetailResponse.FestivalEditorialResponse
import com.peakda.server.domain.festival.presentation.response.FestivalDetailResponse.FestivalHighlightResponse
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalHighlightRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import com.peakda.server.domain.seasonal.entity.BloomCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class FestivalDetailService(
    private val festivalRepository: FestivalRepository,
    private val festivalEditorialRepository: FestivalEditorialRepository,
    private val festivalHighlightRepository: FestivalHighlightRepository,
    private val properties: FestivalDetailProperties,
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {

    @Transactional(readOnly = true)
    fun detail(festivalId: Long, today: LocalDate): FestivalDetailResponse {
        val festival = festivalRepository.findById(festivalId).orElseThrow { FestivalNotFoundException() }
        val editorial = festivalEditorialRepository.findByFestivalIdAndStatus(
            festivalId,
            FestivalEditorialStatus.PUBLISHED,
        )
        val highlights = editorial?.id?.let(festivalHighlightRepository::findByFestivalEditorialIdOrderBySortOrderAsc)
            ?: emptyList()
        val category = BloomCategory.ofFestivalName(festival.name)
        val startsOn = festival.startsOn
        val effectiveEndsOn = startsOn?.let { festival.endsOn ?: it }
        val phase = if (startsOn == null || effectiveEndsOn == null) {
            null
        } else {
            when {
                today.isBefore(startsOn) -> FestivalPhase.UPCOMING
                today.isAfter(effectiveEndsOn) -> FestivalPhase.ENDED
                ChronoUnit.DAYS.between(today, effectiveEndsOn) <= properties.endingSoonDays ->
                    FestivalPhase.ENDING_SOON
                else -> FestivalPhase.ONGOING
            }
        }

        return FestivalDetailResponse(
            festivalId = requireNotNull(festival.id),
            name = festival.name,
            venue = festival.venue,
            roadAddress = festival.roadAddress,
            latitude = festival.latitude,
            longitude = festival.longitude,
            homepageUrl = festival.homepageUrl,
            category = category,
            displayName = category?.displayName,
            startsOn = startsOn,
            endsOn = festival.endsOn,
            durationDays = if (startsOn == null || effectiveEndsOn == null) {
                null
            } else {
                ChronoUnit.DAYS.between(startsOn, effectiveEndsOn).toInt() + 1
            },
            phase = phase,
            dDay = if (phase == FestivalPhase.UPCOMING) {
                ChronoUnit.DAYS.between(today, requireNotNull(startsOn))
            } else {
                null
            },
            endsInDays = if (phase == FestivalPhase.ONGOING || phase == FestivalPhase.ENDING_SOON) {
                ChronoUnit.DAYS.between(today, requireNotNull(effectiveEndsOn))
            } else {
                null
            },
            editorial = editorial?.toResponse(highlights),
        )
    }

    private fun FestivalEditorial.toResponse(
        highlights: List<FestivalHighlight>,
    ): FestivalEditorialResponse = FestivalEditorialResponse(
        hook = hook,
        heroImageUrl = objectKeyUrlResolver.resolve(heroImageUrl),
        periodNote = periodNote,
        placeNote = placeNote,
        admissionFee = admissionFee,
        admissionFeeNote = admissionFeeNote,
        operatingHours = operatingHours,
        operatingHoursNote = operatingHoursNote,
        caution = caution,
        cautionNote = cautionNote,
        directionsTransit = directionsTransit,
        directionsCar = directionsCar,
        highlights = highlights.map { it.toResponse() },
    )

    private fun FestivalHighlight.toResponse(): FestivalHighlightResponse = FestivalHighlightResponse(
        sortOrder = sortOrder,
        title = title,
        body = body,
    )
}
