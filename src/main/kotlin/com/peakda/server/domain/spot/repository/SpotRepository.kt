package com.peakda.server.domain.spot.repository

import com.peakda.server.domain.spot.entity.Spot
import com.peakda.server.domain.spot.entity.SpotType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpotRepository : JpaRepository<Spot, Long> {
    fun findByTypeAndAttractionId(type: SpotType, attractionId: Long): Spot?
    fun findByTypeAndKakaoPlaceId(type: SpotType, kakaoPlaceId: String): Spot?

    /** 명소 id 묶음에 대응하는 (이미 materialize 된) 명소형 Spot 들. 지도 핀에 spotId 를 채울 때 쓴다. */
    fun findByTypeAndAttractionIdIn(type: SpotType, attractionIds: Collection<Long>): List<Spot>

    @Query(
        """
            SELECT s FROM Spot s
            WHERE s.visible = true
              AND s.type = :type
              AND s.latitude BETWEEN :minLat AND :maxLat
              AND s.longitude BETWEEN :minLng AND :maxLng
        """,
    )
    fun findVisibleInBoundingBox(
        @Param("type") type: SpotType,
        @Param("minLat") minLat: Double,
        @Param("maxLat") maxLat: Double,
        @Param("minLng") minLng: Double,
        @Param("maxLng") maxLng: Double,
    ): List<Spot>
}
