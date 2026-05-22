package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import org.springframework.data.jpa.repository.JpaRepository

interface SpotRepository : JpaRepository<Spot, Long> {
    fun findByTypeAndAttractionId(type: SpotType, attractionId: Long): Spot?
    fun findByTypeAndKakaoPlaceId(type: SpotType, kakaoPlaceId: String): Spot?
}
