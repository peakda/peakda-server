package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotRecord
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 스팟 카드에 표시할 대표 이미지 해석기.
 * 명소는 대표 이미지 우선, 이미지가 없으면 최근 게시 기록의 대표 사진을 사용한다.
 */
@Component
class SpotThumbnailResolver(
    private val attractionRepository: AttractionRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordPhotoRepository: SpotRecordPhotoRepository,
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
) {

    fun resolve(spots: Collection<Spot>): Map<Long, String> {
        val attractionThumbnails = attractionThumbnails(spots)
        val fallbackSpots = spots.filter { spot ->
            spot.type == SpotType.LOCAL || requireNotNull(spot.id) !in attractionThumbnails
        }
        return attractionThumbnails + recordThumbnails(fallbackSpots)
    }

    private fun attractionThumbnails(spots: Collection<Spot>): Map<Long, String> {
        val attractionIdBySpot = spots
            .filter { it.type == SpotType.ATTRACTION }
            .mapNotNull { spot -> spot.attractionId?.let { requireNotNull(spot.id) to it } }
        if (attractionIdBySpot.isEmpty()) return emptyMap()

        val imageByAttraction = attractionRepository.findAllById(attractionIdBySpot.map { it.second })
            .mapNotNull { attraction ->
                (attraction.primaryImageUrl ?: attraction.thumbnailImageUrl)?.let { requireNotNull(attraction.id) to it }
            }
            .toMap()

        return attractionIdBySpot.mapNotNull { (spotId, attractionId) ->
            imageByAttraction[attractionId]?.let { spotId to it }
        }.toMap()
    }

    private fun recordThumbnails(spots: Collection<Spot>): Map<Long, String> {
        val spotIds = spots.mapNotNull { it.id }
        if (spotIds.isEmpty()) return emptyMap()

        val records = spotRecordRepository.findBySpotIdInAndStatus(spotIds, SpotRecordStatus.PUBLISHED)
        val latestBySpot = records.groupBy { it.spotId }.mapValues { (_, rows) -> rows.maxBy { it.recordDate } }
        val recordIds = latestBySpot.values.mapNotNull { it.id }
        if (recordIds.isEmpty()) return emptyMap()
        val photosByRecord = spotRecordPhotoRepository.findBySpotRecordIdIn(recordIds)
            .sortedBy { it.sortOrder }
            .groupBy { it.spotRecordId }

        return latestBySpot.mapNotNull { (spotId, record) ->
            val recordId = record.id ?: return@mapNotNull null
            photosByRecord[recordId]?.firstOrNull()?.let { spotId to spotRecordPhotoUploader.presignedUrlOf(it.objectKey) }
        }.toMap()
    }

    private val SpotRecord.recordDate: LocalDate
        get() = visitedDate ?: createdAt.atZone(ZoneOffset.UTC).toLocalDate()
}
