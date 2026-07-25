package com.peakda.server.domain.curation.application

import com.peakda.server.domain.curation.entity.Curation
import com.peakda.server.domain.curation.entity.CurationChapter
import com.peakda.server.domain.curation.entity.CurationRecommendation
import com.peakda.server.domain.curation.entity.CurationStatus
import com.peakda.server.domain.curation.exception.CurationNotFoundException
import com.peakda.server.domain.curation.repository.CurationChapterRepository
import com.peakda.server.domain.curation.repository.CurationRecommendationRepository
import com.peakda.server.domain.curation.repository.CurationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CurationAdminService(
    private val curationRepository: CurationRepository,
    private val curationChapterRepository: CurationChapterRepository,
    private val curationRecommendationRepository: CurationRecommendationRepository,
) {

    /** 주차(weekStartDate) 기준으로 큐레이션을 멱등 등록·수정한다. 챕터·추천은 전량 교체된다. */
    @Transactional
    fun upsert(command: UpsertCurationCommand): Long {
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
        return curationId
    }

    @Transactional
    fun delete(id: Long) {
        val curation = curationRepository.findById(id).orElseThrow { CurationNotFoundException() }
        curationChapterRepository.deleteByCurationId(id)
        curationRecommendationRepository.deleteByCurationId(id)
        curationRepository.delete(curation)
    }

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
