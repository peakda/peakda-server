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

@Service
@Transactional
class SpotFavoriteService(
    private val spotFavoriteRepository: SpotFavoriteRepository,
    private val spotRepository: SpotRepository,
) {

    fun add(userId: Long, spotId: Long): SpotFavoriteResponse {
        val spot = spotRepository.findById(spotId).orElseThrow { SpotNotFoundException() }
        // ON CONFLICT DO NOTHING — 이미 찜한 스팟이면 무시되어 동시 요청에서도 단일 행이 보장된다.
        spotFavoriteRepository.insertIfAbsent(userId, spotId)
        val favorite = requireNotNull(spotFavoriteRepository.findByUserIdAndSpotId(userId, spotId))
        return favorite.toResponse(spot)
    }

    fun remove(userId: Long, spotId: Long) {
        spotFavoriteRepository.deleteByUserIdAndSpotId(userId, spotId)
    }

    /** 사용자의 모든 찜을 삭제한다. 계정 탈퇴 시 사용. */
    fun deleteAllByUser(userId: Long) {
        spotFavoriteRepository.deleteByUserId(userId)
    }

    fun updateNotify(userId: Long, spotId: Long, enabled: Boolean): SpotFavoriteResponse {
        val favorite = spotFavoriteRepository.findByUserIdAndSpotId(userId, spotId)
            ?: throw SpotFavoriteNotFoundException()
        favorite.notifyEnabled = enabled
        val spot = spotRepository.findById(spotId).orElseThrow { SpotNotFoundException() }
        return favorite.toResponse(spot)
    }

    @Transactional(readOnly = true)
    fun list(userId: Long): SpotFavoriteListResponse {
        val favorites = spotFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId)
        val spotsById = spotRepository.findAllById(favorites.map { it.spotId })
            .associateBy { requireNotNull(it.id) }
        val responses = favorites.mapNotNull { favorite ->
            spotsById[favorite.spotId]?.let { favorite.toResponse(it) }
        }
        return SpotFavoriteListResponse(count = responses.size, favorites = responses)
    }

    private fun SpotFavorite.toResponse(spot: Spot): SpotFavoriteResponse = SpotFavoriteResponse(
        spotId = requireNotNull(spot.id),
        type = spot.type,
        name = spot.name,
        address = spot.address,
        attractionId = spot.attractionId,
        notifyEnabled = notifyEnabled,
        favoritedAt = createdAt,
    )
}
