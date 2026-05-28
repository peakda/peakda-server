package com.peakda.server.domain.spot.application

import com.peakda.server.common.page.PageRequest
import com.peakda.server.common.page.PageResponse
import com.peakda.server.common.page.toPageResponse
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotRecordPhoto
import com.peakda.server.domain.spot.entity.SpotRecordPlant
import com.peakda.server.domain.spot.entity.SpotRecordPlantId
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.exception.PlantInactiveException
import com.peakda.server.domain.spot.exception.PlantNotFoundException
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.spot.exception.SpotRecordInvalidStatusException
import com.peakda.server.domain.spot.exception.SpotRecordNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotRecordResponse
import com.peakda.server.domain.spot.presentation.response.SpotRecordSummaryResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordPlantRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class SpotRecordService(
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordPhotoRepository: SpotRecordPhotoRepository,
    private val spotRecordPlantRepository: SpotRecordPlantRepository,
    private val plantRepository: PlantRepository,
    private val spotService: SpotService,
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
    private val responseAssembler: SpotRecordResponseAssembler,
) {

    fun create(command: CreateSpotRecordCommand): SpotRecordResponse {
        validatePlantIds(command.plantIds)
        validatePhotoKeysCount(command.photoKeys.size)
        if (command.status == SpotRecordStatus.PUBLISHED) {
            requirePublishedReady(
                hasVisitedDate = command.visitedDate != null,
                hasBloomStage = command.bloomStage != null,
                plantIds = command.plantIds,
                photoKeys = command.photoKeys,
            )
        }

        val spot = spotService.findOrCreate(command.spot)
        val spotId = requireNotNull(spot.id)
        val now = Instant.now()

        val existingDraft = spotRecordRepository.findByUserIdAndStatus(command.userId, SpotRecordStatus.DRAFT)
        val record = if (existingDraft != null) {
            existingDraft.spotId = spotId
            existingDraft.visitedDate = command.visitedDate
            existingDraft.bloomStage = command.bloomStage
            existingDraft.memo = command.memo
            existingDraft.status = command.status
            existingDraft.publishedAt = if (command.status == SpotRecordStatus.PUBLISHED) now else null
            existingDraft
        } else {
            spotRecordRepository.save(
                SpotRecord(
                    spotId = spotId,
                    userId = command.userId,
                    visitedDate = command.visitedDate,
                    bloomStage = command.bloomStage,
                    memo = command.memo,
                    status = command.status,
                    publishedAt = if (command.status == SpotRecordStatus.PUBLISHED) now else null,
                )
            )
        }

        val recordId = requireNotNull(record.id)
        replacePlants(recordId, command.plantIds)
        replacePhotos(recordId, command.photoKeys)
        return responseAssembler.assemble(record)
    }

    fun update(command: UpdateSpotRecordCommand): SpotRecordResponse {
        val record = loadOwned(command.recordId, command.userId)
        command.visitedDate?.let { record.visitedDate = it }
        command.bloomStage?.let { record.bloomStage = it }
        command.memo?.let { record.memo = it.takeIf { value -> value.isNotEmpty() } }
        command.plantIds?.let {
            validatePlantIds(it)
            replacePlants(command.recordId, it)
        }
        command.photoKeys?.let {
            validatePhotoKeysCount(it.size)
            replacePhotos(command.recordId, it)
        }
        return responseAssembler.assemble(record)
    }

    fun publish(recordId: Long, userId: Long): SpotRecordResponse {
        val record = loadOwned(recordId, userId)
        if (record.status == SpotRecordStatus.PUBLISHED) {
            return responseAssembler.assemble(record)
        }
        val plantIds = spotRecordPlantRepository.findByIdSpotRecordId(recordId).map { it.plantId }
        val photoKeys = spotRecordPhotoRepository.findBySpotRecordIdOrderBySortOrderAsc(recordId).map { it.objectKey }
        requirePublishedReady(
            hasVisitedDate = record.visitedDate != null,
            hasBloomStage = record.bloomStage != null,
            plantIds = plantIds,
            photoKeys = photoKeys,
        )
        record.status = SpotRecordStatus.PUBLISHED
        record.publishedAt = Instant.now()
        return responseAssembler.assemble(record)
    }

    fun delete(recordId: Long, userId: Long) {
        val record = loadOwned(recordId, userId)
        val photos = spotRecordPhotoRepository.findBySpotRecordIdOrderBySortOrderAsc(recordId)
        spotRecordPlantRepository.deleteByIdSpotRecordId(recordId)
        spotRecordPhotoRepository.deleteBySpotRecordId(recordId)
        spotRecordRepository.delete(record)
        photos.forEach { spotRecordPhotoUploader.deleteByMainKey(it.objectKey) }
    }

    @Transactional(readOnly = true)
    fun get(recordId: Long): SpotRecordResponse {
        val record = spotRecordRepository.findById(recordId).orElseThrow { SpotRecordNotFoundException() }
        return responseAssembler.assemble(record)
    }

    @Transactional(readOnly = true)
    fun listBySpot(spotId: Long, pageRequest: PageRequest): PageResponse<SpotRecordSummaryResponse> {
        val pageable = pageRequest.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        val page = spotRecordRepository.findBySpotId(spotId, pageable)
        val summariesById = responseAssembler.assembleSummaries(page.content).associateBy { it.id }
        return page.map { record -> summariesById.getValue(requireNotNull(record.id)) }.toPageResponse()
    }

    @Transactional(readOnly = true)
    fun listMine(userId: Long, status: SpotRecordStatus, pageRequest: PageRequest): PageResponse<SpotRecordSummaryResponse> {
        val pageable = pageRequest.toPageable()
        val page = spotRecordRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
        val summariesById = responseAssembler.assembleSummaries(page.content).associateBy { it.id }
        return page.map { record -> summariesById.getValue(requireNotNull(record.id)) }.toPageResponse()
    }

    private fun loadOwned(recordId: Long, userId: Long): SpotRecord {
        val record = spotRecordRepository.findById(recordId).orElseThrow { SpotRecordNotFoundException() }
        if (record.userId != userId) throw SpotRecordForbiddenException()
        return record
    }

    private fun validatePlantIds(plantIds: List<Long>) {
        if (plantIds.isEmpty()) return
        val distinctIds = plantIds.toSet()
        val plants = plantRepository.findAllById(distinctIds)
        if (plants.size != distinctIds.size) throw PlantNotFoundException()
        if (plants.any { it.status != PlantStatus.ACTIVE }) throw PlantInactiveException()
    }

    private fun validatePhotoKeysCount(size: Int) {
        if (size > 5) throw SpotRecordInvalidStatusException()
    }

    private fun requirePublishedReady(
        hasVisitedDate: Boolean,
        hasBloomStage: Boolean,
        plantIds: List<Long>,
        photoKeys: List<String>,
    ) {
        if (!hasVisitedDate || !hasBloomStage || plantIds.isEmpty() || photoKeys.isEmpty() || photoKeys.size > 5) {
            throw SpotRecordInvalidStatusException()
        }
    }

    private fun replacePlants(recordId: Long, plantIds: List<Long>) {
        spotRecordPlantRepository.deleteByIdSpotRecordId(recordId)
        spotRecordPlantRepository.flush()
        if (plantIds.isEmpty()) return
        val entries = plantIds.distinct().map { SpotRecordPlant(SpotRecordPlantId(recordId, it)) }
        spotRecordPlantRepository.saveAll(entries)
    }

    private fun replacePhotos(recordId: Long, photoKeys: List<String>) {
        val existing = spotRecordPhotoRepository.findBySpotRecordIdOrderBySortOrderAsc(recordId)
        val existingKeys = existing.map { it.objectKey }.toSet()
        val nextKeys = photoKeys.toList()
        val nextKeySet = nextKeys.toSet()
        val orphanedKeys = existingKeys - nextKeySet

        spotRecordPhotoRepository.deleteBySpotRecordId(recordId)
        spotRecordPhotoRepository.flush()
        nextKeys.forEachIndexed { index, key ->
            spotRecordPhotoRepository.save(
                SpotRecordPhoto(spotRecordId = recordId, objectKey = key, sortOrder = index + 1)
            )
        }
        orphanedKeys.forEach { spotRecordPhotoUploader.deleteByMainKey(it) }
    }
}
