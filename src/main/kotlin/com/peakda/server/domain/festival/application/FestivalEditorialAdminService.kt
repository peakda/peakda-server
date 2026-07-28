package com.peakda.server.domain.festival.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.festival.entity.Festival
import com.peakda.server.domain.festival.entity.FestivalEditorial
import com.peakda.server.domain.festival.entity.FestivalEditorialStatus
import com.peakda.server.domain.festival.entity.FestivalHighlight
import com.peakda.server.domain.festival.exception.FestivalEditorialNotFoundException
import com.peakda.server.domain.festival.exception.FestivalNotFoundException
import com.peakda.server.domain.festival.presentation.response.FestivalAdminSummaryResponse
import com.peakda.server.domain.festival.presentation.response.FestivalEditorialAdminResponse
import com.peakda.server.domain.festival.presentation.response.FestivalEditorialAdminResponse.FestivalHighlightAdminResponse
import com.peakda.server.domain.festival.repository.FestivalEditorialRepository
import com.peakda.server.domain.festival.repository.FestivalHighlightRepository
import com.peakda.server.domain.festival.repository.FestivalRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class FestivalEditorialAdminService(
    private val festivalRepository: FestivalRepository,
    private val festivalEditorialRepository: FestivalEditorialRepository,
    private val festivalHighlightRepository: FestivalHighlightRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {

    @Transactional(readOnly = true)
    fun list(query: String?, pageable: Pageable): Page<FestivalAdminSummaryResponse> {
        val normalizedQuery = query?.trim().orEmpty()
        val festivals = if (normalizedQuery.isBlank()) {
            festivalRepository.findAllByOrderByIdDesc(pageable)
        } else {
            festivalRepository.findByNameContainingIgnoreCaseOrderByIdDesc(normalizedQuery, pageable)
        }
        val festivalIds = festivals.content.map { requireNotNull(it.id) }
        val editorialsByFestivalId = if (festivalIds.isEmpty()) {
            emptyMap()
        } else {
            festivalEditorialRepository
                .findByFestivalIdIn(festivalIds)
                .associateBy { it.festivalId }
        }

        return festivals.map { festival ->
            festival.toAdminSummary(
                editorial = editorialsByFestivalId[requireNotNull(festival.id)],
            )
        }
    }

    @Transactional(readOnly = true)
    fun editorial(festivalId: Long): FestivalEditorialAdminResponse {
        val editorial = festivalEditorialRepository.findByFestivalId(festivalId)
            ?: throw FestivalEditorialNotFoundException()
        val editorialId = requireNotNull(editorial.id)
        val highlights = festivalHighlightRepository.findByFestivalEditorialIdOrderBySortOrderAsc(editorialId)
        return editorial.toAdminResponse(highlights)
    }

    /** 축제 id 기준으로 에디토리얼을 멱등 등록·수정하고 주요 볼거리를 전량 교체한다. */
    @Transactional
    fun upsert(adminId: Long, festivalId: Long, command: UpsertFestivalEditorialCommand): Long {
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
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = AdminAuditAction.FESTIVAL_EDITORIAL_UPSERT,
                targetType = AdminAuditTargetType.FESTIVAL,
                targetId = festivalId,
            ),
        )
        return editorialId
    }

    @Transactional
    fun delete(adminId: Long, festivalId: Long) {
        val editorial = festivalEditorialRepository.findByFestivalId(festivalId)
            ?: throw FestivalEditorialNotFoundException()
        val editorialId = requireNotNull(editorial.id)
        festivalHighlightRepository.deleteByFestivalEditorialId(editorialId)
        festivalEditorialRepository.delete(editorial)
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = AdminAuditAction.FESTIVAL_EDITORIAL_DELETE,
                targetType = AdminAuditTargetType.FESTIVAL,
                targetId = festivalId,
            ),
        )
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
            heroImageUrl = heroImageKey,
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
        heroImageUrl = command.heroImageKey
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

    private fun Festival.toAdminSummary(editorial: FestivalEditorial?): FestivalAdminSummaryResponse = FestivalAdminSummaryResponse(
        id = requireNotNull(id),
        name = name,
        venue = venue,
        startsOn = startsOn,
        endsOn = endsOn,
        hasEditorial = editorial != null,
        editorialStatus = editorial?.status,
    )

    private fun FestivalEditorial.toAdminResponse(
        highlights: List<FestivalHighlight>,
    ): FestivalEditorialAdminResponse = FestivalEditorialAdminResponse(
        editorialId = requireNotNull(id),
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
        heroImageKey = heroImageUrl,
        heroImagePreviewUrl = objectKeyUrlResolver.resolve(heroImageUrl),
        status = status,
        publishedAt = publishedAt,
        highlights = highlights.map { highlight ->
            FestivalHighlightAdminResponse(
                sortOrder = highlight.sortOrder,
                title = highlight.title,
                body = highlight.body,
            )
        },
    )
}
