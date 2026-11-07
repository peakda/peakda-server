package com.peakda.server.domain.spot.application

import com.peakda.server.domain.feed.application.FeedReactionService
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.presentation.response.toRecordResponse
import com.peakda.server.domain.spot.presentation.response.toRecordSummaryResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import com.peakda.server.domain.user.application.ProfileImageUrlResolver
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.user.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class SpotRecordResponseAssembler(
    private val spotRepository: SpotRepository,
    private val userRepository: UserRepository,
    private val plantRepository: PlantRepository,
    private val spotRecordPhotoRepository: SpotRecordPhotoRepository,
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val spotRecordReactionRepository: SpotRecordReactionRepository,
    private val spotRecordPhotoUrlResolver: SpotRecordPhotoUrlResolver,
    private val profileImageUrlResolver: ProfileImageUrlResolver,
    private val feedReactionService: FeedReactionService,
) {

    fun assemble(record: SpotRecord, viewerId: Long?): SpotRecordResponse {
        val view = assembleViews(listOf(record), viewerId, includeLegacyReactions = false).single()
        val reactions = feedReactionService.previews(listOf(view.id), viewerId).getValue(view.id)
        return view.toRecordResponse(reactions)
    }

    /** Version-neutral view shared by the v1 and v2 feed adapters. */
    fun assembleView(record: SpotRecord, viewerId: Long?): SpotRecordView {
        val recordId = requireNotNull(record.id) { "record.id must not be null" }
        return buildView(record, loadContext(listOf(record), viewerId), recordId)
    }

    fun assembleViews(records: List<SpotRecord>, viewerId: Long?): List<SpotRecordView> {
        return assembleViews(records, viewerId, includeLegacyReactions = true)
    }

    private fun assembleViews(records: List<SpotRecord>, viewerId: Long?, includeLegacyReactions: Boolean): List<SpotRecordView> {
        if (records.isEmpty()) return emptyList()
        val context = loadContext(records, viewerId, includeLegacyReactions)
        return records.map { record ->
            buildView(record, context, requireNotNull(record.id) { "record.id must not be null" })
        }
    }

    fun assembleSummaries(records: List<SpotRecord>, viewerId: Long?): List<SpotRecordSummaryResponse> {
        if (records.isEmpty()) return emptyList()
        val views = assembleViews(records, viewerId, includeLegacyReactions = false)
        val reactions = feedReactionService.previews(views.map { it.id }, viewerId)
        return views.map { it.toRecordSummaryResponse(reactions.getValue(it.id)) }
    }

    private fun buildView(record: SpotRecord, context: AssemblyContext, recordId: Long): SpotRecordView {
        val spot = context.spotsById.getValue(record.spotId)
        val user = context.usersById.getValue(record.userId)
        val plants = context.plantsByRecordId[recordId].orEmpty()
        val photos = context.photosByRecordId[recordId].orEmpty()
        val legacy = context.reactionsByRecordId.getValue(recordId)
        return SpotRecordView(
            id = recordId,
            spot = SpotView(requireNotNull(spot.id), spot.type, spot.name, spot.address, spot.attractionId),
            user = UserView(requireNotNull(user.id), user.nickname, profileImageUrlResolver.thumbnailUrl(user.profileImageUrl)),
            visitedDate = record.visitedDate,
            bloomStage = record.bloomStage,
            memo = record.memo,
            contentLink = record.contentLinkUrl?.let { url -> record.contentLinkPlatform?.let { ContentLinkView(url, it) } },
            plants = plants.map { PlantView(requireNotNull(it.id), it.name) },
            photos = photos.map { photo ->
                PhotoView(photo.objectKey, spotRecordPhotoUrlResolver.mainUrl(photo.objectKey), photo.sortOrder,
                    spotRecordPhotoUrlResolver.variantUrls(photo.objectKey, photo.variantNames))
            },
            status = record.status,
            publishedAt = record.publishedAt,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            legacyReactions = LegacyReactionOverview(
                counts = legacy.counts,
                myReactions = legacy.myReactions,
            ),
        )
    }

    private fun loadContext(records: List<SpotRecord>, viewerId: Long?, includeLegacyReactions: Boolean = true): AssemblyContext {
        val recordIds = records.mapNotNull { it.id }
        val spotIds = records.map { it.spotId }.toSet()
        val userIds = records.map { it.userId }.toSet()
        val spotsById = spotRepository.findAllById(spotIds).associateBy { requireNotNull(it.id) }
        val usersById = userRepository.findAllById(userIds).associateBy { requireNotNull(it.id) }
        val photosByRecordId = spotRecordPhotoRepository.findBySpotRecordIdIn(recordIds)
            .sortedBy { it.sortOrder }
            .groupBy { it.spotRecordId }
        val joins = spotRecordPlantRepository.findByIdSpotRecordIdIn(recordIds)
        val plantIds = joins.map { it.plantId }.toSet()
        val plantsById = plantRepository.findAllById(plantIds).associateBy { requireNotNull(it.id) }
        val plantsByRecordId = joins
            .mapNotNull { join -> plantsById[join.plantId]?.let { join.spotRecordId to it } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, plants) -> plants.sortedBy { it.sortOrder } }
        val countsByRecordId = (if (includeLegacyReactions) spotRecordReactionRepository.countsBySpotRecordIdIn(recordIds) else emptyList())
            .groupBy { it.spotRecordId }
            .mapValues { (_, counts) -> counts.map { LegacyReactionCount(it.reactionType, it.count) } }
        val mineByRecordId = viewerId?.takeIf { includeLegacyReactions }?.let {
            spotRecordReactionRepository.findByUserIdAndSpotRecordIdIn(it, recordIds)
                .groupBy { reaction -> reaction.spotRecordId }
                .mapValues { (_, reactions) -> reactions.map { reaction -> reaction.reactionType }.toSet() }
        }.orEmpty()
        val reactionsByRecordId = recordIds.associateWith { recordId ->
            LegacyReactionOverview(
                counts = countsByRecordId[recordId].orEmpty(),
                myReactions = mineByRecordId[recordId].orEmpty(),
            )
        }
        return AssemblyContext(spotsById, usersById, plantsByRecordId, photosByRecordId, reactionsByRecordId)
    }

    private data class AssemblyContext(
        val spotsById: Map<Long, Spot>,
        val usersById: Map<Long, User>,
        val plantsByRecordId: Map<Long, List<Plant>>,
        val photosByRecordId: Map<Long, List<SpotRecordPhoto>>,
        val reactionsByRecordId: Map<Long, LegacyReactionOverview>,
    )
}
