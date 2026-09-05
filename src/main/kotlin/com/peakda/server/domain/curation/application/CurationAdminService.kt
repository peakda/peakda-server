package com.peakda.server.domain.curation.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.admin.application.AdminAuditRecorder
import com.peakda.server.domain.admin.application.RecordAdminAuditCommand
import com.peakda.server.domain.admin.entity.AdminAuditAction
import com.peakda.server.domain.admin.entity.AdminAuditTargetType
import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationChapter
import com.peakda.server.domain.curation.entity.CurationRecommendation
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.exception.CurationNotFoundException
import com.peakda.server.domain.curation.presentation.response.CurationAdminDetailResponse
import com.peakda.server.domain.curation.presentation.response.CurationAdminDetailResponse.CurationAdminChapterResponse
import com.peakda.server.domain.curation.presentation.response.CurationAdminDetailResponse.CurationAdminRecommendationResponse
import com.peakda.server.domain.curation.presentation.response.CurationAdminSummaryResponse
import com.peakda.server.domain.curation.repository.CurationChapterRepository
import com.peakda.server.domain.curation.repository.CurationChildCounts
import com.peakda.server.domain.curation.repository.CurationRecommendationRepository
import com.peakda.server.domain.curation.repository.CurationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CurationAdminService(
    private val curationRepository: CurationRepository,
    private val curationChapterRepository: CurationChapterRepository,
    private val curationRecommendationRepository: CurationRecommendationRepository,
    private val adminAuditRecorder: AdminAuditRecorder,
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {

    /** 백오피스 큐레이션 목록. 상태 필터가 없으면 모든 상태를 최신 주차순으로 조회한다. */
    @Transactional(readOnly = true)
    fun list(status: CurationStatus?, pageable: Pageable): Page<CurationAdminSummaryResponse> {
        val page = if (status == null) {
            curationRepository.findAllByOrderByWeekStartDateDesc(pageable)
        } else {
            curationRepository.findByStatusOrderByWeekStartDateDesc(status, pageable)
        }
        val curationIds = page.content.map { requireNotNull(it.id) }
        val childCounts = childCountsByCurationId(curationIds)
        return page.map { curation ->
            val curationId = requireNotNull(curation.id)
            val counts = childCounts[curationId]
            curation.toSummaryResponse(
                chapterCount = counts?.chapterCount ?: 0L,
                recommendationCount = counts?.recommendationCount ?: 0L,
            )
        }
    }

    /** 백오피스 큐레이션 상세. DRAFT/PUBLISHED 모두 조회하고 저장된 하위 항목 순서를 보존한다. */
    @Transactional(readOnly = true)
    fun detail(id: Long): CurationAdminDetailResponse {
        val curation = curationRepository.findCurationById(id) ?: throw CurationNotFoundException()
        val chapters = curationChapterRepository.findByCurationIdOrderBySortOrderAsc(id)
        val recommendations = curationRecommendationRepository.findByCurationIdOrderBySortOrderAsc(id)
        return curation.toDetailResponse(chapters, recommendations)
    }

    /** 주차(weekStartDate) 기준으로 큐레이션을 멱등 등록·수정한다. 챕터·추천은 전량 교체된다. */
    @Transactional
    fun upsert(adminId: Long, command: UpsertCurationCommand): Long {
        val existing = curationRepository.findByWeekStartDate(command.weekStartDate)
        val curation = if (existing == null) {
            curationRepository.save(command.toEntity())
        } else {
            existing.applyCommand(command)
        }
        val curationId = requireNotNull(curation.id)

        curationChapterRepository.deleteByCurationId(curationId)
        curationRecommendationRepository.deleteByCurationId(curationId)
        curationChapterRepository.saveAll(
            command.chapters.mapIndexed { index, chapter -> chapter.toEntity(curationId, index + 1) },
        )
        curationRecommendationRepository.saveAll(
            command.recommendations.mapIndexed { index, recommendation -> recommendation.toEntity(curationId, index + 1) },
        )
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = AdminAuditAction.CURATION_UPSERT,
                targetType = AdminAuditTargetType.CURATION,
                targetId = curationId,
            ),
        )
        return curationId
    }

    @Transactional
    fun delete(adminId: Long, id: Long) {
        val curation = curationRepository.findById(id).orElseThrow { CurationNotFoundException() }
        curationChapterRepository.deleteByCurationId(id)
        curationRecommendationRepository.deleteByCurationId(id)
        curationRepository.delete(curation)
        adminAuditRecorder.record(
            RecordAdminAuditCommand(
                adminId = adminId,
                action = AdminAuditAction.CURATION_DELETE,
                targetType = AdminAuditTargetType.CURATION,
                targetId = id,
            ),
        )
    }

    private fun childCountsByCurationId(curationIds: List<Long>): Map<Long, CurationChildCounts> {
        if (curationIds.isEmpty()) return emptyMap()
        return curationRepository.countChildrenByCurationIdIn(curationIds)
            .associateBy { it.curationId }
    }

    private fun Curation.toSummaryResponse(
        chapterCount: Long,
        recommendationCount: Long,
    ): CurationAdminSummaryResponse = CurationAdminSummaryResponse(
        id = requireNotNull(id),
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        weekLabel = weekLabel,
        title = title,
        status = status,
        publishedAt = publishedAt,
        chapterCount = chapterCount,
        recommendationCount = recommendationCount,
    )

    private fun Curation.toDetailResponse(
        chapters: List<CurationChapter>,
        recommendations: List<CurationRecommendation>,
    ): CurationAdminDetailResponse = CurationAdminDetailResponse(
        id = requireNotNull(id),
        status = status,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        title = title,
        subtitle = subtitle,
        heroImageKey = heroImageUrl,
        heroImagePreviewUrl = objectKeyUrlResolver.resolve(heroImageUrl),
        intro = intro,
        nextTeaserOverline = nextTeaserOverline,
        nextTeaserBody = nextTeaserBody,
        publishedAt = publishedAt,
        chapters = chapters.map { it.toAdminResponse() },
        recommendations = recommendations.map { it.toAdminResponse() },
    )

    private fun CurationChapter.toAdminResponse(): CurationAdminChapterResponse =
        CurationAdminChapterResponse(
            sortOrder = sortOrder,
            layout = layout,
            heading = heading,
            spotId = spotId,
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            photoKey = photoUrl,
            photoPreviewUrl = objectKeyUrlResolver.resolve(photoUrl),
            pullQuote = pullQuote,
            leadText = leadText,
            body = body,
            factNote = factNote,
        )

    private fun CurationRecommendation.toAdminResponse(): CurationAdminRecommendationResponse =
        CurationAdminRecommendationResponse(
            sortOrder = sortOrder,
            title = title,
            spotId = spotId,
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            photoKey = photoUrl,
            photoPreviewUrl = objectKeyUrlResolver.resolve(photoUrl),
            body = body,
        )

    private fun UpsertCurationCommand.toEntity(): Curation = Curation(
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        weekLabel = weekLabel,
        heroImageUrl = heroImageUrl,
        title = title,
        subtitle = subtitle,
        intro = intro,
        nextTeaserOverline = nextTeaserOverline,
        nextTeaserBody = nextTeaserBody,
        status = status,
        publishedAt = if (status == CurationStatus.PUBLISHED) Instant.now() else null,
    )

    private fun Curation.applyCommand(command: UpsertCurationCommand): Curation {
        weekEndDate = command.weekEndDate
        weekLabel = command.weekLabel
        heroImageUrl = command.heroImageUrl
        title = command.title
        subtitle = command.subtitle
        intro = command.intro
        nextTeaserOverline = command.nextTeaserOverline
        nextTeaserBody = command.nextTeaserBody
        publishedAt = when {
            command.status == CurationStatus.DRAFT -> null
            status == CurationStatus.PUBLISHED -> publishedAt
            else -> Instant.now()
        }
        status = command.status
        return this
    }

    private fun UpsertCurationChapterCommand.toEntity(curationId: Long, sortOrder: Int): CurationChapter =
        CurationChapter(
            curationId = curationId,
            sortOrder = sortOrder,
            layout = layout,
            heading = heading,
            spotId = spotId,
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            photoUrl = photoUrl,
            pullQuote = pullQuote,
            leadText = leadText,
            body = body,
            factNote = factNote,
        )

    private fun UpsertCurationRecommendationCommand.toEntity(
        curationId: Long,
        sortOrder: Int,
    ): CurationRecommendation = CurationRecommendation(
        curationId = curationId,
        sortOrder = sortOrder,
        title = title,
        spotId = spotId,
        placeName = placeName,
        latitude = latitude,
        longitude = longitude,
        photoUrl = photoUrl,
        body = body,
    )
}
