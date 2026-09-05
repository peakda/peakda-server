package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.seasonal.entity.BloomStatus
import com.peakda.server.domain.seasonal.entity.SeasonalBloomEstimate
import com.peakda.server.domain.seasonal.repository.SeasonalBloomEstimateRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.entity.SpotRecordStatus
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteListResponse
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteListResponse.BloomBanner
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteResponse
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteResponse.Bloom
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteResponse.CategoryChip
import com.peakda.server.domain.spot.repository.SpotRecordPhotoRepository
import com.peakda.server.domain.spot.repository.SpotRecordRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class SpotFavoriteListAssembler(
    private val seasonalBloomEstimateRepository: SeasonalBloomEstimateRepository,
    private val spotRecordRepository: SpotRecordRepository,
    private val spotRecordPhotoRepository: SpotRecordPhotoRepository,
    private val spotRecordPhotoUploader: SpotRecordPhotoUploader,
    private val attractionRepository: AttractionRepository,
    private val properties: SpotFavoriteProperties,
) {

    fun assemble(
        favorites: List<SpotFavorite>,
        spotsById: Map<Long, Spot>,
        today: LocalDate,
    ): SpotFavoriteListResponse {
        val cards = favorites.mapNotNull { favorite ->
            spotsById[favorite.spotId]?.let { spot -> FavoriteCard(favorite, spot) }
        }
        if (cards.isEmpty()) {
            return SpotFavoriteListResponse(count = 0, banner = null, favorites = emptyList())
        }

        val spotIds = cards.map { it.spotId }
        val recordCountBySpot = spotRecordRepository
            .countBySpotIdInAndStatus(spotIds, SpotRecordStatus.PUBLISHED)
            .associate { it.spotId to it.recordCount }
        val photoUrlsBySpot = recentPhotoUrls(spotIds)
        val bloomByAttraction = bloomsByAttraction(cards, today)
        val fallbackImageByAttraction = fallbackImages(cards, photoUrlsBySpot)

        val responses = cards.map { card ->
            val bloomAttractionId = card.bloomAttractionId
            val bloomData = bloomAttractionId?.let { bloomByAttraction[it] }
            SpotFavoriteResponse(
                spotId = card.spotId,
                type = card.spot.type,
                name = card.spot.name,
                address = card.spot.address,
                attractionId = card.spot.attractionId,
                notifyEnabled = card.favorite.notifyEnabled,
                favoritedAt = card.favorite.createdAt,
                bloom = bloomData?.bloom,
                categories = bloomData?.categories.orEmpty(),
                recordCount = recordCountBySpot[card.spotId] ?: 0,
                photoUrls = photoUrlsBySpot[card.spotId]
                    ?: bloomAttractionId?.let { fallbackImageByAttraction[it] }?.let(::listOf)
                    ?: emptyList(),
            )
        }

        return SpotFavoriteListResponse(
            count = responses.size,
            banner = selectBanner(responses, today),
            favorites = responses,
        )
    }

    private fun recentPhotoUrls(spotIds: List<Long>): Map<Long, List<String>> {
        if (properties.photoLimit <= 0) return emptyMap()
        return spotRecordPhotoRepository
            .findRecentPhotosBySpotIds(
                spotIds = spotIds,
                status = SpotRecordStatus.PUBLISHED.name,
                limit = properties.photoLimit,
            )
            .groupBy { it.spotId }
            .mapValues { (_, photos) ->
                photos.take(properties.photoLimit).map { spotRecordPhotoUploader.presignedUrlOf(it.objectKey) }
            }
    }

    private fun bloomsByAttraction(
        cards: List<FavoriteCard>,
        today: LocalDate,
    ): Map<Long, BloomData> {
        val attractionIds = cards.mapNotNull { it.bloomAttractionId }.distinct()
        if (attractionIds.isEmpty()) return emptyMap()

        val baseDate = seasonalBloomEstimateRepository.findLatestBaseDate() ?: return emptyMap()
        return seasonalBloomEstimateRepository
            .findByBaseDateAndAttractionIdIn(baseDate, attractionIds)
            .filter { it.status != BloomStatus.ENDED }
            .groupBy { it.attractionId }
            .mapValues { (_, estimates) ->
                val ordered = estimates.sortedWith(
                    compareBy<SeasonalBloomEstimate>({ statusRank(it.status) }, { -it.confidence }),
                )
                BloomData(
                    bloom = ordered.first().toBloom(baseDate, today),
                    categories = ordered
                        .distinctBy { it.bloomCategory }
                        .map { CategoryChip(it.bloomCategory, it.bloomCategory.displayName) },
                )
            }
    }

    /**
     * 기록 사진이 없는 명소형 카드만 명소 대표 이미지로 대체해 빈 사진 영역을 피한다.
     * 대체가 필요한 카드가 없으면 명소 저장소를 조회하지 않는다.
     */
    private fun fallbackImages(
        cards: List<FavoriteCard>,
        photoUrlsBySpot: Map<Long, List<String>>,
    ): Map<Long, String> {
        val fallbackAttractionIds = cards
            .filter { photoUrlsBySpot[it.spotId].isNullOrEmpty() }
            .mapNotNull { it.bloomAttractionId }
            .distinct()
        if (fallbackAttractionIds.isEmpty()) return emptyMap()

        return attractionRepository.findAllById(fallbackAttractionIds)
            .mapNotNull { attraction ->
                val imageUrl = attraction.primaryImageUrl ?: attraction.thumbnailImageUrl
                imageUrl?.let { requireNotNull(attraction.id) to it }
            }
            .toMap()
    }

    private fun selectBanner(
        favorites: List<SpotFavoriteResponse>,
        today: LocalDate,
    ): BloomBanner? {
        val candidates = favorites.filter { it.bloom?.peakStartDate != null }
        // 절정 판정은 추정 당시의 status 가 아니라 오늘이 만개 구간 안인지로 한다(결정 C 산식).
        // 산출일이 하루 이상 지난 추정은 status 가 아직 PREPARING 이어도 오늘 이미 절정일 수 있고,
        // status 로만 거르면 만개가 오늘 시작하는 스팟이 배너에서 통째로 빠진다.
        val currentPeak = candidates
            .filter { favorite ->
                val bloom = requireNotNull(favorite.bloom)
                requireNotNull(bloom.peakStartDate) <= today &&
                    (bloom.peakEndDate == null || today <= bloom.peakEndDate)
            }
            .minWithOrNull(
                compareBy<SpotFavoriteResponse>(
                    { it.bloom?.peakEndDate ?: LocalDate.MAX },
                    { it.spotId },
                ),
            )
        if (currentPeak != null) {
            return currentPeak.toBanner(SpotFavoriteBannerMessage.currentPeak(currentPeak.name))
        }

        return candidates
            .filter {
                val daysUntilPeak = requireNotNull(it.bloom?.daysUntilPeak)
                daysUntilPeak in 1..properties.bannerLeadDays
            }
            .minWithOrNull(
                compareBy<SpotFavoriteResponse>(
                    { it.bloom?.daysUntilPeak },
                    { it.spotId },
                ),
            )
            ?.let { it.toBanner(SpotFavoriteBannerMessage.imminent(it.name)) }
    }

    private fun SpotFavoriteResponse.toBanner(message: String): BloomBanner {
        val bloom = requireNotNull(bloom)
        return BloomBanner(
            spotId = spotId,
            spotName = name,
            category = bloom.category,
            displayName = bloom.displayName,
            status = bloom.status,
            peakStartDate = requireNotNull(bloom.peakStartDate),
            peakEndDate = bloom.peakEndDate,
            daysUntilPeak = requireNotNull(bloom.daysUntilPeak),
            message = message,
        )
    }

    private fun SeasonalBloomEstimate.toBloom(baseDate: LocalDate, today: LocalDate): Bloom {
        val daysUntilPeak = peakStartDate?.let { ChronoUnit.DAYS.between(today, it) }
        return Bloom(
            category = bloomCategory,
            displayName = bloomCategory.displayName,
            status = status,
            peakStartDate = peakStartDate,
            peakEndDate = peakEndDate,
            peakDurationDays = peakDurationDays,
            daysUntilPeak = daysUntilPeak,
            imminent = daysUntilPeak?.let { it in 1..properties.bannerLeadDays } ?: false,
            baseDate = baseDate,
        )
    }

    private data class FavoriteCard(
        val favorite: SpotFavorite,
        val spot: Spot,
    ) {
        val spotId: Long = requireNotNull(spot.id)
        val bloomAttractionId: Long? = spot.attractionId.takeIf { spot.type == SpotType.ATTRACTION }
    }

    private data class BloomData(
        val bloom: Bloom,
        val categories: List<CategoryChip>,
    )

    companion object {
        private fun statusRank(status: BloomStatus): Int = when (status) {
            BloomStatus.PEAK -> 0
            BloomStatus.STARTED -> 1
            BloomStatus.PREPARING -> 2
            BloomStatus.ENDED -> 3
        }
    }
}
