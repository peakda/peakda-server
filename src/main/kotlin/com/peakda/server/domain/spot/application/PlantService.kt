package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.Plant
import com.peakda.server.domain.spot.entity.PlantStatus
import com.peakda.server.domain.spot.exception.PlantSuggestionDuplicateException
import com.peakda.server.domain.spot.exception.PlantSuggestionRateLimitException
import com.peakda.server.domain.spot.presentation.response.PlantResponse
import com.peakda.server.domain.spot.repository.PlantRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlantService(
    private val plantRepository: PlantRepository,
    private val plantSuggestionRateLimiter: PlantSuggestionRateLimiter,
) {

    @Transactional(readOnly = true)
    fun listActive(): List<PlantResponse> =
        plantRepository.findAllByStatusOrderBySortOrderAscIdAsc(PlantStatus.ACTIVE).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun search(keyword: String): List<PlantResponse> {
        val normalized = normalize(keyword)
        if (normalized.isEmpty()) return emptyList()
        return plantRepository
            .findAllByStatusAndNameContainingIgnoreCaseOrderBySortOrderAscIdAsc(PlantStatus.ACTIVE, normalized)
            .map { it.toResponse() }
    }

    fun suggest(command: SuggestPlantCommand): PlantResponse {
        val name = normalize(command.name)
        if (name.isEmpty()) throw PlantSuggestionDuplicateException()
        if (!plantSuggestionRateLimiter.tryAcquire(command.userId)) throw PlantSuggestionRateLimitException()
        if (plantRepository.existsByNameIgnoreCase(name)) throw PlantSuggestionDuplicateException()

        val saved = plantRepository.save(
            Plant(
                name = name,
                sortOrder = 0,
                status = PlantStatus.PENDING,
                suggestedByUserId = command.userId,
            )
        )
        return saved.toResponse()
    }

    private fun normalize(value: String): String = value.trim().replace(WHITESPACE_REGEX, " ")

    private fun Plant.toResponse() = PlantResponse(
        id = requireNotNull(id),
        name = name,
        status = status,
        seasons = seasons.sorted(),
    )

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
