package com.peakda.server.domain.spot.application

import com.peakda.server.domain.attraction.entity.Attraction
import com.peakda.server.domain.attraction.repository.AttractionRepository
import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import com.peakda.server.domain.spot.exception.AttractionNotFoundException
import com.peakda.server.domain.spot.exception.SpotNotFoundException
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SpotService(
    private val spotRepository: SpotRepository,
    private val attractionRepository: AttractionRepository,
) {

    @Transactional(readOnly = true)
    fun getById(id: Long): Spot =
        spotRepository.findById(id).orElseThrow { SpotNotFoundException() }

    @Transactional
    fun findOrCreateForAttraction(attraction: Attraction): Spot {
        val attractionId = requireNotNull(attraction.id) { "attraction.id must not be null" }
        spotRepository.findByTypeAndAttractionId(SpotType.ATTRACTION, attractionId)?.let { return it }
        val newSpot = Spot(
            type = SpotType.ATTRACTION,
            attractionId = attractionId,
            name = attraction.title,
            address = attraction.addressMain,
            latitude = requireNotNull(attraction.latitude) { "attraction.latitude must not be null" },
            longitude = requireNotNull(attraction.longitude) { "attraction.longitude must not be null" },
        )
        return spotRepository.save(newSpot)
    }

    @Transactional
    fun findOrCreate(input: SpotResolveInput): Spot {
        input.existingSpotId?.let { id ->
            return spotRepository.findById(id).orElseThrow { SpotNotFoundException() }
        }
        return when (input.type) {
            SpotType.ATTRACTION -> resolveAttractionSpot(input)
            SpotType.LOCAL -> resolveLocalSpot(input)
        }
    }

    private fun resolveAttractionSpot(input: SpotResolveInput): Spot {
        val attractionId = input.attractionId ?: throw AttractionNotFoundException()
        val attraction = attractionRepository.findById(attractionId)
            .orElseThrow { AttractionNotFoundException() }
        return findOrCreateForAttraction(attraction)
    }

    private fun resolveLocalSpot(input: SpotResolveInput): Spot {
        input.kakaoPlaceId?.takeIf { it.isNotBlank() }?.let { kpid ->
            spotRepository.findByTypeAndKakaoPlaceId(SpotType.LOCAL, kpid)?.let { return it }
        }
        val newSpot = Spot(
            type = SpotType.LOCAL,
            attractionId = null,
            name = input.name,
            address = input.address,
            latitude = input.latitude,
            longitude = input.longitude,
            kakaoPlaceId = input.kakaoPlaceId?.takeIf { it.isNotBlank() },
            createdByUserId = input.userId,
        )
        return spotRepository.save(newSpot)
    }
}
