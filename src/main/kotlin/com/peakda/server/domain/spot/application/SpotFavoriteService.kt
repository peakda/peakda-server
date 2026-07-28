package com.peakda.server.domain.spot.application

import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotFavorite
import com.peakda.server.domain.spot.exception.SpotFavoriteNotFoundException
import com.peakda.server.domain.spot.exception.SpotNotFoundException
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteListResponse
import com.peakda.server.domain.spot.presentation.response.SpotFavoriteResponse
import com.peakda.server.domain.spot.repository.SpotFavoriteRepository
import com.peakda.server.domain.spot.repository.SpotRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional
class SpotFavoriteService(
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val spotRepository: SpotRepository,
    private val spotFavoriteListAssembler: SpotFavoriteListAssembler,
) {

    fun add(userId: Long, spotId: Long, today: LocalDate): SpotFavoriteResponse {
        val spot = spotRepository.findById(spotId).orElseThrow { SpotNotFoundException() }
        // ON CONFLICT DO NOTHING — 이미 찜한 스팟이면 무시되어 동시 요청에서도 단일 행이 보장된다.
        spotFavoriteRepository.insertIfAbsent(userId, spotId)
        val favorite = requireNotNull(spotFavoriteRepository.findByUserIdAndSpotId(userId, spotId))
        return assembleSingle(favorite, spot, today)
    }

    fun remove(userId: Long, spotId: Long) {
        spotFavoriteRepository.deleteByUserIdAndSpotId(userId, spotId)
    }

    /** 사용자의 모든 찜을 삭제한다. 계정 탈퇴 시 사용. */
    fun deleteAllByUser(userId: Long) {
        spotFavoriteRepository.deleteByUserId(userId)
    }

    fun updateNotify(userId: Long, spotId: Long, enabled: Boolean, today: LocalDate): SpotFavoriteResponse {
        val favorite = spotFavoriteRepository.findByUserIdAndSpotId(userId, spotId)
            ?: throw SpotFavoriteNotFoundException()
        favorite.notifyEnabled = enabled
        val spot = spotRepository.findById(spotId).orElseThrow { SpotNotFoundException() }
        return assembleSingle(favorite, spot, today)
    }

    @Transactional(readOnly = true)
    fun list(userId: Long, today: LocalDate): SpotFavoriteListResponse {
        val favorites = spotFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
        if (favorites.isEmpty()) {
            return spotFavoriteListAssembler.assemble(emptyList(), emptyMap(), today)
        }
        val spotsById = spotRepository.findAllById(favorites.map { it.spotId })
            .associateBy { requireNotNull(it.id) }
        return spotFavoriteListAssembler.assemble(favorites, spotsById, today)
    }

    private fun assembleSingle(favorite: SpotFavorite, spot: Spot, today: LocalDate): SpotFavoriteResponse =
        spotFavoriteListAssembler
            .assemble(
                favorites = listOf(favorite),
                spotsById = mapOf(requireNotNull(spot.id) to spot),
                today = today,
            )
            .favorites
            .single()
}
