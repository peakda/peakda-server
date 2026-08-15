package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import com.peakda.server.domain.feed.presentation.response.ReactionCount
import com.peakda.server.domain.feed.presentation.response.ReactionSummary
import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse.PhotoEntry
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse.PlantSummary
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse.SpotSummary
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse.UserSummary
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordReactionRepository
import com.peakda.server.domain.spot.repository.SpotRepository
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
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {

    fun assemble(record: SpotRecord, viewerId: Long): SpotRecordResponse {
        val recordId = requireNotNull(record.id) { "record.id must not be null" }
        val context = loadContext(listOf(record), viewerId)
        return buildResponse(record, context, recordId)
    }

    fun assembleSummaries(records: List<SpotRecord>, viewerId: Long): List<SpotRecordSummaryResponse> {
        if (records.isEmpty()) return emptyList()
        val context = loadContext(records, viewerId)
        return records.map { record ->
            val recordId = requireNotNull(record.id) { "record.id must not be null" }
            buildSummary(record, context, recordId)
        }
    }

    private fun buildResponse(record: SpotRecord, context: AssemblyContext, recordId: Long): SpotRecordResponse {
        val spot = context.spotsById.getValue(record.spotId)
        val user = context.usersById.getValue(record.userId)
        val plants = context.plantsByRecordId[recordId].orEmpty()
        val photos = context.photosByRecordId[recordId].orEmpty()
        return SpotRecordResponse(
            id = recordId,
            spot = spot.toSummary(),
            user = user.toSummary(),
            visitedDate = record.visitedDate,
            bloomStage = record.bloomStage,
            memo = record.memo,
            plants = plants.map { it.toSummary() },
            photos = photos.map { it.toEntry() },
            status = record.status,
            publishedAt = record.publishedAt,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            reactions = context.reactionsByRecordId.getValue(recordId),
        )
    }

    private fun buildSummary(record: SpotRecord, context: AssemblyContext, recordId: Long): SpotRecordSummaryResponse {
        val spot = context.spotsById.getValue(record.spotId)
        val user = context.usersById.getValue(record.userId)
        val plants = context.plantsByRecordId[recordId].orEmpty()
        val cover = context.photosByRecordId[recordId]?.firstOrNull()
        return SpotRecordSummaryResponse(
            id = recordId,
            spotId = spot.id!!,
            spotName = spot.name,
            user = user.toSummary(),
            visitedDate = record.visitedDate,
            bloomStage = record.bloomStage,
            memo = record.memo,
            plants = plants.map { it.toSummary() },
            coverPhoto = cover?.toEntry(),
            status = record.status,
            publishedAt = record.publishedAt,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            reactions = context.reactionsByRecordId.getValue(recordId),
        )
    }

    private fun loadContext(records: List<SpotRecord>, viewerId: Long): AssemblyContext {
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
        val countsByRecordId = spotRecordReactionRepository.countsBySpotRecordIdIn(recordIds)
            .groupBy { it.spotRecordId }
            .mapValues { (_, counts) -> counts.map { ReactionCount(it.reactionType, it.count) } }
        val mineByRecordId = spotRecordReactionRepository.findByUserIdAndSpotRecordIdIn(viewerId, recordIds)
            .groupBy { it.spotRecordId }
            .mapValues { (_, reactions) -> reactions.map { it.reactionType }.toSet() }
        val reactionsByRecordId = recordIds.associateWith { recordId ->
            ReactionSummary(
                counts = countsByRecordId[recordId].orEmpty(),
                myReactions = mineByRecordId[recordId].orEmpty(),
            )
        }
        return AssemblyContext(spotsById, usersById, plantsByRecordId, photosByRecordId, reactionsByRecordId)
    }

    private fun Spot.toSummary() = SpotSummary(
        id = requireNotNull(id),
        type = type,
        name = name,
        address = address,
        attractionId = attractionId,
    )

    private fun User.toSummary() = UserSummary(
        id = requireNotNull(id),
        nickname = nickname,
        profileImageUrl = objectKeyUrlResolver.resolve(profileImageUrl),
    )

    private fun Plant.toSummary() = PlantSummary(id = requireNotNull(id), name = name)

    private fun SpotRecordPhoto.toEntry() = PhotoEntry(
        objectKey = objectKey,
        url = spotRecordPhotoUploader.presignedUrlOf(objectKey),
        sortOrder = sortOrder,
    )

    private data class AssemblyContext(
        val spotsById: Map<Long, Spot>,
        val usersById: Map<Long, User>,
        val plantsByRecordId: Map<Long, List<Plant>>,
        val photosByRecordId: Map<Long, List<SpotRecordPhoto>>,
        val reactionsByRecordId: Map<Long, ReactionSummary>,
    )
}
