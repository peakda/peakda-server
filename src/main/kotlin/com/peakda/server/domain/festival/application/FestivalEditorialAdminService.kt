package com.peakda.server.domain.festival.application

import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalHighlight
import com.peakda.server.domain.festival.exception.FestivalEditorialNotFoundException
import com.peakda.server.domain.festival.exception.FestivalNotFoundException
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalHighlightRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class FestivalEditorialAdminService(
    private val festivalRepository: FestivalRepository,
    private val festivalEditorialRepository: FestivalEditorialRepository,
    private val festivalHighlightRepository: FestivalHighlightRepository,
) {

    /** 축제 id 기준으로 에디토리얼을 멱등 등록·수정하고 주요 볼거리를 전량 교체한다. */
    @Transactional
    fun upsert(festivalId: Long, command: UpsertFestivalEditorialCommand): Long {
        if (!festivalRepository.existsById(festivalId)) {
            throw FestivalNotFoundException()
        }
        val existing = festivalEditorialRepository.findByFestivalId(festivalId)
        val editorial = if (existing == null) {
            festivalEditorialRepository.save(command.toEntity(festivalId))
        } else {
            existing.applyCommand(command)
        }
        val editorialId = requireNotNull(editorial.id)

        festivalHighlightRepository.deleteByFestivalEditorialId(editorialId)
        festivalHighlightRepository.saveAll(
            command.highlights.mapIndexed { index, highlight -> highlight.toEntity(editorialId, index + 1) },
        )
        return editorialId
    }

    @Transactional
    fun delete(festivalId: Long) {
        val editorial = festivalEditorialRepository.findByFestivalId(festivalId)
            ?: throw FestivalEditorialNotFoundException()
        val editorialId = requireNotNull(editorial.id)
        festivalHighlightRepository.deleteByFestivalEditorialId(editorialId)
        festivalEditorialRepository.delete(editorial)
    }

    private fun UpsertFestivalEditorialCommand.toEntity(festivalId: Long): FestivalEditorial =
        FestivalEditorial(
            festivalId = festivalId,
            hook = hook,
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
            heroImageUrl = heroImageUrl,
            status = status,
            publishedAt = if (status == FestivalEditorialStatus.PUBLISHED) Instant.now() else null,
        )

    private fun FestivalEditorial.applyCommand(command: UpsertFestivalEditorialCommand): FestivalEditorial {
        hook = command.hook
        periodNote = command.periodNote
        placeNote = command.placeNote
        admissionFee = command.admissionFee
        admissionFeeNote = command.admissionFeeNote
        operatingHours = command.operatingHours
        operatingHoursNote = command.operatingHoursNote
        caution = command.caution
        cautionNote = command.cautionNote
        directionsTransit = command.directionsTransit
        directionsCar = command.directionsCar
        heroImageUrl = command.heroImageUrl
        publishedAt = when {
            command.status == FestivalEditorialStatus.DRAFT -> null
            status == FestivalEditorialStatus.PUBLISHED -> publishedAt
            else -> Instant.now()
        }
        status = command.status
        return this
    }

    private fun UpsertFestivalHighlightCommand.toEntity(
        festivalEditorialId: Long,
        sortOrder: Int,
    ): FestivalHighlight = FestivalHighlight(
        festivalEditorialId = festivalEditorialId,
        sortOrder = sortOrder,
        title = title,
        body = body,
    )
}
